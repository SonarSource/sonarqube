/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.activity.db;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.activity.Activity;
import org.sonar.core.activity.ActivityLog;
import org.sonar.core.persistence.Dto;

/**
 * @since 4.4
 */
public final class ActivityDto extends Dto<ActivityKey> {

  private String message;
  private Activity.Type type;
  private String author;

  private String data;

  protected ActivityDto() {
  }

  @Override
  public ActivityKey getKey() {
    return ActivityKey.of(this.getCreatedAt(), type, author);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
  }

  public Activity.Type getType() {
    return type;
  }

  public ActivityDto setType(Activity.Type type) {
    this.type = type;
    return this;
  }

  public String getAuthor() {
    return author;
  }

  public ActivityDto setAuthor(String author) {
    this.author = author;
    return this;
  }

  public ActivityDto setExecutionTime(Integer executionTime) {
    return this;
  }

  public String getData() {
    return data;
  }

  public ActivityDto setData(String data) {
    this.data = data;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public ActivityDto setMessage(String message) {
    this.message = message;
    return this;
  }

  public static ActivityDto createFor(String message) {
    return new ActivityDto()
      .setMessage(message);
  }

  public static ActivityDto createFor(ActivityLog activityLog) {
    return new ActivityDto()
      .setData(KeyValueFormat.format(activityLog.getDetails()));
  }
}
