package org.ottobackwards.hdfs;

import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
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
    Builder builder = new Builder(new MockNotifier()).withConfiguration(new Configuration())
        .withLastTransactionId(0L).withTargets(targets);
    HdfsNotificationListener listener = builder.build();
    Assert.assertNotNull(listener);
  }

  @Test
  public void testShortHappyPath() {
    Builder builder = new Builder(new MockNotifier()).withConfiguration(new Configuration());
    HdfsNotificationListener listener = builder.build();
    Assert.assertNotNull(listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoConfigurationThrowsException() {
    Builder builder = new Builder(new MockNotifier()).withLastTransactionId(0L)
        .withTargets(targets);
    builder.build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoNotifierThrowsException() {
    Builder builder = new Builder(null).withLastTransactionId(0L)
        .withConfiguration(new Configuration()).withTargets(targets);
    builder.build();
  }

}