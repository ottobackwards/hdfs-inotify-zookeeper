/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ottobackwards.zookeeper;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /root
 * ->reg-area
 * -->entry
 * --->hdfsWatchPath | value => hdfs path
 * --->notifyNode | value => notification json or empty "{}"
 */
public class ZookeeperScanner {
  private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ZookeeperScanner() {
  }

  public static List<ZookeeperNotificationTarget> scan(String zookeeperUrl,
      String registrationPath) throws Exception {
    if (StringUtils.isEmpty(zookeeperUrl)) {
      throw new IllegalArgumentException("zookeeperUrl cannot be null or empty");
    }
    if (StringUtils.isEmpty(registrationPath)) {
      throw new IllegalArgumentException("registrationPath cannot be null or empty");
    }
    try (CuratorFramework framework = getClient(zookeeperUrl)) {
      framework.start();
      return scan(framework, registrationPath);
    }
  }

  public static List<ZookeeperNotificationTarget> scan(CuratorFramework client,
      String registrationPath) throws Exception {
    if (client == null) {
      throw new IllegalArgumentException("client cannot be null");
    }
    if (StringUtils.isEmpty(registrationPath)) {
      throw new IllegalArgumentException("registrationPath cannot be null or empty");
    }
    final List<ZookeeperNotificationTarget> targets = new ArrayList<>();
    // TODO: refactor with to support withWatcher and watch?
    List<String> entries = client.getChildren().forPath(registrationPath);
    for (String entryNodePath : entries) {
      Optional<ZookeeperNotificationTarget> target = createTargetForEntry(client,registrationPath, entryNodePath);
      target.ifPresent((zookeeperNotificationTarget -> targets.add(zookeeperNotificationTarget)));
    }
    return targets;
  }

  public static CuratorFramework getClient(String zookeeperUrl) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    return CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
  }

  private static Optional<ZookeeperNotificationTarget> createTargetForEntry(CuratorFramework client,
     String parentPath, String entryNodePath) throws Exception {
    List<String> entryNodes = client.getChildren().forPath(String.format("%s/%s",parentPath, entryNodePath));
    if (entryNodes.size() < 2) {
      LOG.error(String.format("Found %d entryNodes", entryNodes.size()));
      return Optional.empty();
    }

    ZookeeperNotificationTarget target = null;

    for (String node : entryNodes) {
      if (node.equals("hdfsWatchPath")) {
        byte[] data = client.getData().forPath(String.format("%s/%s/%s",parentPath,entryNodePath,node));
        target = new ZookeeperNotificationTarget(new String(data));
      }
    }
    return Optional.ofNullable(target);
  }
}
