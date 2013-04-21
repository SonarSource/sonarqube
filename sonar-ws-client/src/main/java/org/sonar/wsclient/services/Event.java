/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

import java.util.Date;

public class Event extends Model {

  private String id;
  private String name;
  private String category;
  private String description;
  private String resourceKey;
  private Date date;
  private String data;

  public String getId() {
    return id;
  }

  public Event setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Event setName(String name) {
    this.name = name;
    return this;
  }

  public String getCategory() {
    return category;
  }

  public Event setCategory(String category) {
    this.category = category;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Event setDescription(String description) {
    this.description = description;
    return this;
  }

  public Date getDate() {
    return date;
  }

  public Event setDate(Date date) {
    this.date = date;
    return this;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public Event setResourceKey(String resourceKey) {
    this.resourceKey = resourceKey;
    return this;
  }

  public String getData() {
    return data;
  }

  public Event setData(String data) {
    this.data = data;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Event event = (Event) o;
    return !(id != null ? !id.equals(event.id) : event.id != null);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
