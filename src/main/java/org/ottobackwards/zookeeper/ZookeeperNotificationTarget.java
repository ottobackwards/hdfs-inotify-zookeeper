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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.regex.Pattern;

public class ZookeeperNotificationTarget {

  public static class Builder {
    private String zkPath;
    private String watchPath;

    public Builder withZkPath(String zkPath) {
      this.zkPath = zkPath;
      return this;
    }

    public Builder withWatchPath(String watchPath) {
      this.watchPath = watchPath;
      return this;
    }

    public ZookeeperNotificationTarget build() {
      if (StringUtils.isEmpty(zkPath)) {
        throw new IllegalArgumentException("zkPath cannot be empty");
      }

      if (StringUtils.isEmpty(watchPath)) {
        throw new IllegalArgumentException("watchPath cannot be empty");
      }
      return new ZookeeperNotificationTarget(watchPath, zkPath);
    }
  }

  private String zkPath;
  private Pattern pattern;
  private String watchPath;

  private ZookeeperNotificationTarget(String watchPath, String zkPath) {
    this.zkPath = zkPath;
    this.watchPath = watchPath;
  }

  public String getWatchPath() {
    return watchPath;
  }

  public String getZkPath() {
    return zkPath;
  }

  public boolean matches(String potentialMatch) {
    if (pattern == null) {
      pattern = Pattern.compile(watchPath);
    }
    return pattern.matcher(potentialMatch).matches();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ZookeeperNotificationTarget that = (ZookeeperNotificationTarget) o;

    return new EqualsBuilder().append(getZkPath(), that.getZkPath())
            .append(getWatchPath(), that.getWatchPath()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getZkPath()).append(getWatchPath()).toHashCode();
  }
}
