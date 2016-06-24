/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.activity;

import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class ActivityDto {

  private String key;
  private String profileKey;
  private String message;
  private String type;
  private String action;
  private String author;
  private String data;
  private Date createdAt;

  public ActivityDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  public String getProfileKey() {
    return profileKey;
  }

  public ActivityDto setProfileKey(String profileKey) {
    this.profileKey = profileKey;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ActivityDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getType() {
    return type;
  }

  public ActivityDto setType(String type) {
    this.type = type;
    return this;
  }

  public String getAuthor() {
    return author;
  }

  public ActivityDto setAuthor(@Nullable String author) {
    this.author = author;
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

  public ActivityDto setMessage(@Nullable String message) {
    this.message = message;
    return this;
  }

  public String getAction() {
    return action;
  }

  public ActivityDto setAction(String action) {
    this.action = action;
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
