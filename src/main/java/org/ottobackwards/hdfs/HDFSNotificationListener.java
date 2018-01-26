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
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSInotifyEventInputStream;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hdfs.inotify.Event;
import org.apache.hadoop.hdfs.inotify.Event.CreateEvent;
import org.apache.hadoop.hdfs.inotify.Event.UnlinkEvent;
import org.apache.hadoop.hdfs.inotify.EventBatch;
import org.apache.hadoop.hdfs.inotify.MissingEventsException;
import org.ottobackwards.zookeeper.ZookeeperNotificationTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HDFSNotificationListener {
  private static Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private long lastTransactionId;
  private String hdfsUri;


  public HDFSNotificationListener(String hdfsUri) {
    this(hdfsUri,0);
  }

  public HDFSNotificationListener(String hdfsUri, long lastTransactionId) {
    this.hdfsUri = hdfsUri;
    this.lastTransactionId = lastTransactionId;
  }

  public long getLastTransactionId() {
    return lastTransactionId;
  }

  public void start() throws IOException, MissingEventsException, InterruptedException {
    HdfsAdmin admin = new HdfsAdmin(URI.create(hdfsUri), new Configuration());

    DFSInotifyEventInputStream eventStream = admin.getInotifyEventStream(lastTransactionId);

    while (true) {
      EventBatch batch = eventStream.take();
      LOG.trace("TransactionId = " + batch.getTxid());
      lastTransactionId = batch.getTxid();
      for (Event event : batch.getEvents()) {
        LOG.trace("event type = " + event.getEventType());
        String path = getPath(event);
        LOG.trace("event path = " + path);
        // evaluate if we want to evaluate this path
        Optional<ZookeeperNotificationTarget> notificationTarget = getTargetForPath(path);
        notificationTarget.ifPresent((target) -> {
          String notification = createNotification(event);

          sendNotification(notificationTarget.get(), notification);

        });
      }
    }
  }


  private String getPath(Event event) {
    if (event == null || event.getEventType() == null) {
      throw new IllegalArgumentException("Event and event type must not be null.");
    }

    switch (event.getEventType()) {
      case CREATE: return ((Event.CreateEvent) event).getPath();
      case CLOSE: return ((Event.CloseEvent) event).getPath();
      case APPEND: return ((Event.AppendEvent) event).getPath();
      case RENAME: return ((Event.RenameEvent) event).getSrcPath();
      case METADATA: return ((Event.MetadataUpdateEvent) event).getPath();
      case UNLINK: return ((Event.UnlinkEvent) event).getPath();
      default: throw new IllegalArgumentException("Unsupported event type.");
    }
  }

  private Optional<ZookeeperNotificationTarget> getTargetForPath(String path) {
    if(StringUtils.isEmpty(path)) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    // validate against each target

    return Optional.empty();
  }

  private String createNotification(Event event) {
    switch (event.getEventType()) {
      case CREATE:
        CreateEvent createEvent = (CreateEvent) event;
        LOG.trace("  path = " + createEvent.getPath());
        LOG.trace("  owner = " + createEvent.getOwnerName());
        LOG.trace("  ctime = " + createEvent.getCtime());
        break;
      case UNLINK:
        UnlinkEvent unlinkEvent = (UnlinkEvent) event;
        LOG.trace("  path = " + unlinkEvent.getPath());
        LOG.trace("  timestamp = " + unlinkEvent.getTimestamp());
        break;

      case APPEND:
      case CLOSE:
      case RENAME:
      case METADATA:
      default:
        break;
    }
    return null;
  }

  private void sendNotification(ZookeeperNotificationTarget target, String notification) {

  }
}
