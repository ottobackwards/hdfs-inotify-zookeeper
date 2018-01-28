package org.ottobackwards.zookeeper;

import org.apache.curator.framework.CuratorFramework;

public class DefaultZookeeperNotifier implements ZookeeperNotifier{
    private CuratorFramework client;

    public DefaultZookeeperNotifier(){}
    public DefaultZookeeperNotifier(CuratorFramework client) {
        this.client = client;
    }

    @Override
    public void notify(ZookeeperNotificationTarget target, String notification) throws Exception{
        if(client != null) {
            client.setData().forPath(target.getZkPath(), notification.getBytes());
        }
    }
}
