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

package org.ottobackwards.hdfs;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DFSInotifyEventInputStream;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hdfs.inotify.Event;
import org.apache.hadoop.hdfs.inotify.Event.AppendEvent;
import org.apache.hadoop.hdfs.inotify.Event.CreateEvent;
import org.apache.hadoop.hdfs.inotify.Event.UnlinkEvent;
import org.apache.hadoop.hdfs.inotify.EventBatch;
import org.json.simple.JSONObject;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;
import org.ottobackwards.zookeeper.ZookeeperNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdfsNotificationListener {

  private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static class Builder {

    private long lastTransactionId = 0L;
    private final List<ZookeeperNotificationTarget> targetsList = new ArrayList<>();
    private ZookeeperNotifier notifier;
    private Configuration configuration;
    public Builder(ZookeeperNotifier notifier) {
      if (notifier == null) {
        throw new IllegalArgumentException("notifier cannot be null");
      }
      this.notifier = notifier;
    }

    public Builder withLastTransactionId(long id) {
      this.lastTransactionId = id;
      return this;
    }

    public Builder withTargets(Collection<? extends ZookeeperNotificationTarget> targets) {
      targetsList.addAll(targets);
      return this;
    }

    public Builder withConfiguration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public HdfsNotificationListener build() {
      if (configuration == null) {
        throw new IllegalArgumentException("hdfsUri and configuration cannot be null");
      }
      return new HdfsNotificationListener(configuration, notifier, lastTransactionId, targetsList);
    }
  }


  private long lastTransactionId;
  private ZookeeperNotifier notifier;
  private final List<ZookeeperNotificationTarget> targetsList = new ArrayList<>();
  private AtomicBoolean stopFlag = new AtomicBoolean(false);
  private Configuration configuration;

  private HdfsNotificationListener(Configuration configuration,ZookeeperNotifier notifier, long lastTransactionId,
      List<ZookeeperNotificationTarget> targets) {
    this.configuration = configuration;
    this.notifier = notifier;
    this.lastTransactionId = lastTransactionId;
    this.targetsList.addAll(targets);
  }
  public long getLastTransactionId() {
    return lastTransactionId;
  }

  public void start() {
    new Thread(() -> {
      LOG.trace("HdfsNotificationListener started");
      if (configuration == null) {
        configuration = new Configuration();
      }

      try {
        URI hdfsUri = FileSystem.newInstance(configuration).getUri();
        HdfsAdmin admin = new HdfsAdmin(hdfsUri, configuration);

        DFSInotifyEventInputStream eventStream = admin.getInotifyEventStream(lastTransactionId);

        while (!stopFlag.get()) {
          EventBatch batch = eventStream.take();
          LOG.trace("TransactionId = " + batch.getTxid());
          lastTransactionId = batch.getTxid();
          for (Event event : batch.getEvents()) {
            if (isEventSupported(event)) {
              LOG.trace("Supported event type = " + event.getEventType());
              String path = getPath(event);
              LOG.trace("event path = " + path);
              // evaluate if we want to evaluate this path
              handleEvent(path, event);
            }
          }
        }
        LOG.trace("HdfsNotificationListener stopped");
      } catch (Exception e) {
        LOG.error("Error processing events", e);
      }
    }).start();
  }

  public void stop() {
    LOG.trace("Stop HdfsNotificationListener");
    stopFlag.set(true);
  }

  public void addZookeeperNotificationTarget(ZookeeperNotificationTarget target) {
    synchronized (targetsList) {
      if (!targetsList.contains(target)) {
        targetsList.add(target);
      }
    }
  }

  private boolean isEventSupported(Event event) {
    boolean ret = false;
    switch (event.getEventType()) {
      case CREATE:
      case APPEND:
      case UNLINK:
        ret = true;
        break;
      default:
        ret = false;
    }
    return ret;
  }

  private String getPath(Event event) {
    if (event == null || event.getEventType() == null) {
      throw new IllegalArgumentException("Event and event type must not be null.");
    }

    switch (event.getEventType()) {
      case CREATE:
        return ((Event.CreateEvent) event).getPath();
      case CLOSE:
        return ((Event.CloseEvent) event).getPath();
      case APPEND:
        return ((Event.AppendEvent) event).getPath();
      case RENAME:
        return ((Event.RenameEvent) event).getSrcPath();
      case METADATA:
        return ((Event.MetadataUpdateEvent) event).getPath();
      case UNLINK:
        return ((Event.UnlinkEvent) event).getPath();
      default:
        throw new IllegalArgumentException("Unsupported event type.");
    }
  }

  private void handleEvent(String path, Event event) throws Exception {
    synchronized (targetsList) {
      for (ZookeeperNotificationTarget target : targetsList) {
        if (target.matches(path)) {
          notifier.notify(target, createNotification(event));
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private String createNotification(Event event) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("eventType", event.getEventType().toString());
    switch (event.getEventType()) {
      case CREATE:
        CreateEvent createEvent = (CreateEvent) event;
        jsonObject.put("path", createEvent.getPath());
        jsonObject.put("owner", createEvent.getOwnerName());
        jsonObject.put("overwrite", createEvent.getOverwrite());
        jsonObject.put("ctime", createEvent.getCtime());
        LOG.trace("  path = " + createEvent.getPath());
        LOG.trace("  owner = " + createEvent.getOwnerName());
        LOG.trace("  ctime = " + createEvent.getCtime());
        break;
      case UNLINK:
        UnlinkEvent unlinkEvent = (UnlinkEvent) event;
        jsonObject.put("path", unlinkEvent.getPath());
        jsonObject.put("timestamp", unlinkEvent.getTimestamp());
        LOG.trace("  path = " + unlinkEvent.getPath());
        LOG.trace("  timestamp = " + unlinkEvent.getTimestamp());
        break;
      case APPEND:
        AppendEvent appendEvent = (AppendEvent) event;
        jsonObject.put("path", appendEvent.getPath());
        LOG.trace("  path = " + appendEvent.getPath());
        break;
      case CLOSE:
      case RENAME:
      case METADATA:
      default:
        break;
    }
    return jsonObject.toJSONString();
  }
}
