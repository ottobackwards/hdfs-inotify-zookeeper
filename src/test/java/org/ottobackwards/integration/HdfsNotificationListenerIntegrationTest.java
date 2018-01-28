package org.ottobackwards.integration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.junit.Assert;
import org.junit.Test;
import org.ottobackwards.MockConnectedNotifier;
import org.ottobackwards.hdfs.HdfsNotificationListener;
import org.ottobackwards.hdfs.HdfsNotificationListener.Builder;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;
import org.ottobackwards.zookeeper.ZookeeperScanner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;

public class HdfsNotificationListenerIntegrationTest {

  @Test
  public void test() throws Exception {
    MRComponent component = new MRComponent().withBasePath("target/hdfs");
    component.start();
    Configuration configuration = component.getConfiguration();
    try (FileSystem fileSystem = FileSystem.newInstance(configuration)) {
      // setup the file
      URI uri = fileSystem.getUri();
      fileSystem.mkdirs(new Path("/work/"),
              new FsPermission(FsAction.READ_WRITE, FsAction.READ_WRITE, FsAction.READ_WRITE));
      fileSystem.copyFromLocalFile(new Path("./src/test/resources/foo"), new Path("/work/"));

      // setup zookeeper

      try (TestingServer testingServer = new TestingServer()) {
        testingServer.start();
        String zkString = testingServer.getConnectString();
        try (CuratorFramework client = ZookeeperScanner.getClient(zkString)) {
          client.start();
          MockConnectedNotifier notifier = new MockConnectedNotifier(client);
          publishTestNodes(client);
          try (TreeCache cache = TreeCache.newBuilder(client, "/test/registration/foo").build()) {
            cache.getListenable().addListener(new TreeCacheListener() {
              @Override
              public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent)
                      throws Exception {
                System.out.println("TREECACHE: " + treeCacheEvent.getType());
              }
            });

            cache.start();

            List<ZookeeperNotificationTarget> targets = ZookeeperScanner
                    .scan(client, "/test/registration");
            Assert.assertNotNull(targets);
            Assert.assertEquals(1, targets.size());
            Assert.assertTrue(targets.get(0).matches("/work/foo"));

            HdfsNotificationListener listener = new Builder(notifier).withConfiguration(configuration)
                    .withLastTransactionId(3L)
                    .withTargets(targets)
                    .build();
            listener.start();
            // change the file
            Path file = new Path("/work/foo");
            if (fileSystem.exists(file)) {
              // UNLINK
              fileSystem.delete(file, true);
            }
            //CREATE
            OutputStream os = fileSystem.create(file);
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            br.write("Hello World");
            br.close();
            Thread.sleep(2000L);
            listener.stop();
            Assert.assertEquals(notifier.getNotifications().size(), 2);
            Assert.assertTrue(notifier.getNotifications().get(0).contains("UNLINK"));
            Assert.assertTrue(notifier.getNotifications().get(1).contains("CREATE"));

          }
        }
      }

    } catch (IOException e) {
      throw new RuntimeException("Unable to start cluster", e);
    }
  }

  private static void publishTestNodes(CuratorFramework client) throws Exception {
    client.create().creatingParentsIfNeeded()
            .forPath("/test/registration/foo/hdfsWatchPath", "/work/foo".getBytes());
    client.create().creatingParentsIfNeeded()
            .forPath("/test/registration/foo/notifyNode", "{}".getBytes());
  }
}
