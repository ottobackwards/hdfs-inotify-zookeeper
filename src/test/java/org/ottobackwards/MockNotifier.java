package org.ottobackwards;

import java.util.ArrayList;
import java.util.List;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;
import org.ottobackwards.zookeeper.ZookeeperNotifier;

public class MockNotifier implements ZookeeperNotifier{

  List<ZookeeperNotificationTarget> targets = new ArrayList<>();
  List<String> notifications = new ArrayList<>();
  @Override
  public void notify(ZookeeperNotificationTarget target, String notification) {
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
