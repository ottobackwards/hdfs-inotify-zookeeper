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

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ZookeeperNotificationTarget {
  private String path;
  private Pattern pattern;

  public ZookeeperNotificationTarget(String path) {
    if (StringUtils.isEmpty(path)) {
      throw new IllegalArgumentException("path cannot be null or empty");
    }
    this.path = path;
  }


  public String getPath() {
    return path;
  }

  public boolean matches(String potentialMatch) {
    if (pattern == null) {
      pattern = Pattern.compile(path);
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

    return new EqualsBuilder().append(getPath(), that.getPath()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getPath()).toHashCode();
  }
}
