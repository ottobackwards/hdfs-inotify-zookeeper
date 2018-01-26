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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSInotifyEventInputStream;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hdfs.inotify.Event;
import org.apache.hadoop.hdfs.inotify.Event.AppendEvent;
import org.apache.hadoop.hdfs.inotify.Event.CreateEvent;
import org.apache.hadoop.hdfs.inotify.Event.UnlinkEvent;
import org.apache.hadoop.hdfs.inotify.EventBatch;
import org.apache.hadoop.hdfs.inotify.MissingEventsException;
import org.json.simple.JSONObject;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HDFSNotificationListener {

  private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static class Builder {
    private long lastTransactionId = 0L;
    private URI hdfsUri;
    private List<ZookeeperNotificationTarget> targetsList;

    public Builder(URI hdfsUri){
      if(hdfsUri == null) {
        throw new IllegalArgumentException("hdfsUri cannot be null");
      }
    }

    public Builder withLastTransactionId(long id){
      this.lastTransactionId = id;
      return this;
    }

    public Builder withTargets(Collection<? extends ZookeeperNotificationTarget> targets) {
      targetsList.addAll(targets);
      return this;
    }

    public HDFSNotificationListener build() {
      if(hdfsUri == null) {
        throw new IllegalArgumentException("hdfsUri cannot be null");
      }
      return new HDFSNotificationListener(hdfsUri, lastTransactionId, targetsList);
    }
  }


  private long lastTransactionId;
  private URI hdfsUri;
  private List<ZookeeperNotificationTarget> targetsList;
  private AtomicBoolean stopFlag = new AtomicBoolean(false);

  private HDFSNotificationListener(URI hdfsUri, long lastTransactionId, List<ZookeeperNotificationTarget> targets) {
    this.hdfsUri = hdfsUri;
    this.lastTransactionId = lastTransactionId;
    this.targetsList = targets;
  }

  public long getLastTransactionId() {
    return lastTransactionId;
  }

  public void start() throws IOException, MissingEventsException, InterruptedException {
    HdfsAdmin admin = new HdfsAdmin(hdfsUri, new Configuration());

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
          Optional<ZookeeperNotificationTarget> notificationTarget = getTargetForPath(path);
          LOG.trace("path " + path + " is tracked");
          notificationTarget.ifPresent((target) -> {
            String notification = createNotification(event);
            sendNotification(notificationTarget.get(), notification);
          });
        }
      }
    }
    LOG.trace("HDFSNotificationListener stopped");
  }

  public void stop() {
    LOG.trace("Stop HDFSNotificationListener");
    stopFlag.set(true);
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

  private Optional<ZookeeperNotificationTarget> getTargetForPath(String path) {
    if (StringUtils.isEmpty(path)) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    // validate against each target

    return Optional.empty();
  }

  private String createNotification(Event event) {
    JSONObject eventJSON = new JSONObject();
    switch (event.getEventType()) {
      case CREATE:
        CreateEvent createEvent = (CreateEvent) event;
        eventJSON.put("path", createEvent.getPath());
        eventJSON.put("owner", createEvent.getOwnerName());
        eventJSON.put("overwrite", createEvent.getOverwrite());
        eventJSON.put("ctime", createEvent.getCtime());
        LOG.trace("  path = " + createEvent.getPath());
        LOG.trace("  owner = " + createEvent.getOwnerName());
        LOG.trace("  ctime = " + createEvent.getCtime());
        break;
      case UNLINK:
        UnlinkEvent unlinkEvent = (UnlinkEvent) event;
        eventJSON.put("path", unlinkEvent.getPath());
        eventJSON.put("overwrite", unlinkEvent.getTimestamp());
        LOG.trace("  path = " + unlinkEvent.getPath());
        LOG.trace("  timestamp = " + unlinkEvent.getTimestamp());
        break;
      case APPEND:
        AppendEvent appendEvent = (AppendEvent) event;
        eventJSON.put("path", appendEvent.getPath());
        LOG.trace("  path = " + appendEvent.getPath());
        break;
      case CLOSE:
      case RENAME:
      case METADATA:
      default:
        break;
    }
    return eventJSON.toJSONString();
  }

  private void sendNotification(ZookeeperNotificationTarget target, String notification) {

  }
}
