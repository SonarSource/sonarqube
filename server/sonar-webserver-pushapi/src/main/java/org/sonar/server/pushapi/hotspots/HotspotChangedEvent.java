/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.pushapi.hotspots;

import java.io.Serializable;
import java.util.Date;
import javax.annotation.CheckForNull;

public class HotspotChangedEvent implements Serializable {

  private final String key;
  private final String projectKey;
  private final Long updateDate;
  private final String status;
  private final String assignee;
  private final String resolution;
  private final String filePath;

  private HotspotChangedEvent(Builder builder) {
    this.key = builder.getKey();
    this.projectKey = builder.getProjectKey();
    this.updateDate = builder.getUpdateDate() == null ? null : builder.getUpdateDate().getTime();
    this.status = builder.getStatus();
    this.filePath = builder.getFilePath();
    this.resolution = builder.getResolution();
    this.assignee = builder.getAssignee();
  }

  public String getKey() {
    return key;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public Long getUpdateDate() {
    return updateDate;
  }

  public String getStatus() {
    return status;
  }

  public String getFilePath() {
    return filePath;
  }

  @CheckForNull
  public String getResolution() {
    return resolution;
  }

  @CheckForNull
  public String getAssignee() {
    return assignee;
  }

  public static class Builder {
    private String key;
    private String projectKey;
    private Date updateDate;
    private String status;
    private String assignee;
    private String resolution;
    private String filePath;

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setProjectKey(String projectKey) {
      this.projectKey = projectKey;
      return this;
    }

    public Builder setUpdateDate(Date updateDate) {
      this.updateDate = updateDate;
      return this;
    }

    public Builder setStatus(String status) {
      this.status = status;
      return this;
    }

    public Builder setAssignee(String assignee) {
      this.assignee = assignee;
      return this;
    }

    public Builder setResolution(String resolution) {
      this.resolution = resolution;
      return this;
    }

    public Builder setFilePath(String filePath) {
      this.filePath = filePath;
      return this;
    }

    public String getKey() {
      return key;
    }

    public String getProjectKey() {
      return projectKey;
    }

    public Date getUpdateDate() {
      return updateDate;
    }

    public String getStatus() {
      return status;
    }

    public String getAssignee() {
      return assignee;
    }

    public String getResolution() {
      return resolution;
    }

    public String getFilePath() {
      return filePath;
    }

    public HotspotChangedEvent build() {
      return new HotspotChangedEvent(this);
    }
  }
}
