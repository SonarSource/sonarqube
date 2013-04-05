/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.api.issue;

import java.util.Date;

/**
 * @since 3.6
 */
public class IssueChangelog {

  private String severity;
  private String status;
  private String resolution;
  private String message;
  private Integer line;

  private Date createdAt;

  private IssueChangelog(Builder builder) {
    this.severity = builder.severity;
    this.status = builder.status;
    this.resolution = builder.resolution;
    this.message = builder.message;
    this.line = builder.line;
    this.createdAt = builder.createdAt;
  }

  public String severity() {
    return severity;
  }

  public String status() {
    return status;
  }

  public String resolution() {
    return resolution;
  }

  public String message() {
    return message;
  }

  public Integer line() {
    return line;
  }

  public Date createdAt() {
    return createdAt;
  }

  /**
   * @since 3.6
   */
  public static class Builder {
    private String severity;
    private String status;
    private String resolution;
    private String message;
    private Integer line;

    private Date createdAt;

    public Builder() {
      createdAt = new Date();
    }

    public Builder severity(String severity) {
      this.severity = severity;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder resolution(String resolution) {
      this.resolution = resolution;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder line(Integer line) {
      this.line = line;
      return this;
    }

    public IssueChangelog build() {
      return new IssueChangelog(this);
    }
  }
}
