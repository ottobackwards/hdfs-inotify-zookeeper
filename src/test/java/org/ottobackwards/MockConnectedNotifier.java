package org.ottobackwards;

import java.util.ArrayList;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.ottobackwards.zookeeper.DefaultZookeeperNotifier;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;
import org.ottobackwards.zookeeper.ZookeeperNotifier;

public class MockConnectedNotifier extends DefaultZookeeperNotifier{

  List<ZookeeperNotificationTarget> targets = new ArrayList<>();
  List<String> notifications = new ArrayList<>();

  public MockConnectedNotifier(CuratorFramework client) {
    super(client);
  }

  @Override
  public void notify(ZookeeperNotificationTarget target, String notification) throws Exception {
    super.notify(target,notification);
    targets.add(target);
    notifications.add(notification);
  }

  public List<String> getNotifications() {
    return notifications;
  }

  public List<ZookeeperNotificationTarget> getTargets() {
    return targets;
  }
}
