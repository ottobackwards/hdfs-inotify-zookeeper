package org.ottobackwards.integration;

import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Test;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;
import org.ottobackwards.zookeeper.ZookeeperScanner;

public class ZookeeperScannerIntegrationTest {

  @Test
  public void testScannerWithPath() throws Exception {
    try (TestingServer testingServer = new TestingServer()) {
      testingServer.start();
      String zkString = testingServer.getConnectString();
      try (CuratorFramework client = ZookeeperScanner.getClient(zkString)) {
        client.start();
        publishTestNodes(client);
      }
      List<ZookeeperNotificationTarget> targets = ZookeeperScanner.scan(zkString, "/test/registration");
      Assert.assertNotNull(targets);
      Assert.assertEquals(1, targets.size());
      Assert.assertTrue(targets.get(0).matches("/bar/grok"));
      Assert.assertFalse(targets.get(0).matches("/bar/grok/wha"));
    }
  }

  private static void publishTestNodes(CuratorFramework client) throws Exception {
    client.create().creatingParentsIfNeeded()
        .forPath("/test/registration/foo/hdfsWatchPath", "/bar/grok".getBytes());
    client.create().creatingParentsIfNeeded()
        .forPath("/test/registration/foo/notifyNode", "{}".getBytes());
  }
}
