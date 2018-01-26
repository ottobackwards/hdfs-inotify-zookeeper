package org.ottobackwards.hdfs;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ottobackwards.MockNotifier;
import org.ottobackwards.hdfs.HdfsNotificationListener.Builder;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;

public class HdfsNotificationListenerBuilderTest {

  private List<ZookeeperNotificationTarget> targets;

  @Before
  public void before() {
    targets = new ArrayList<>();
    targets.add(new ZookeeperNotificationTarget("foo/bar"));
  }

  @Test
  public void testHappyPath() {
    Builder builder = new Builder(URI.create("hdfs://foo:8020"), new MockNotifier())
        .withLastTransactionId(0L).withTargets(targets);
    HdfsNotificationListener listener = builder.build();
    Assert.assertNotNull(listener);
  }

  @Test
  public void testShortHappyPath() {
    Builder builder = new Builder(URI.create("hdfs://foo:8020"), new MockNotifier());
    HdfsNotificationListener listener = builder.build();
    Assert.assertNotNull(listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoUriThrowsException() {
    Builder builder = new Builder(null, new MockNotifier()).withLastTransactionId(0L)
        .withTargets(targets);
    HdfsNotificationListener listener = builder.build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoNotifierThrowsException() {
    Builder builder = new Builder(URI.create("hdfs://foo:8020"), null).withLastTransactionId(0L)
        .withTargets(targets);
    HdfsNotificationListener listener = builder.build();
  }

}