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
package org.sonar.wsclient.services;

import java.util.Date;

public class EventQuery extends Query<Event> {
  public static final String BASE_URL = "/api/events";

  private String resourceKey;
  private String[] categories;
  private Date fromDate;
  private boolean includeFromTime;
  private Date toDate;
  private boolean includeToTime;

  public EventQuery() {
  }

  public EventQuery(String resourceKey) {
    this.resourceKey = resourceKey;
  }

  public Date getFrom() {
    return fromDate;
  }

  public EventQuery setFrom(Date fromDate, boolean includeTime) {
    this.fromDate = fromDate;
    this.includeFromTime = includeTime;
    return this;
  }

  public boolean isIncludeFromTime() {
    return includeFromTime;
  }

  public Date getTo() {
    return toDate;
  }

  public EventQuery setTo(Date toDate, boolean includeTime) {
    this.toDate = toDate;
    this.includeToTime = includeTime;
    return this;
  }

  public boolean isIncludeToTime() {
    return includeToTime;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public EventQuery setResourceKey(String resourceKey) {
    this.resourceKey = resourceKey;
    return this;
  }

  public String[] getCategories() {
    return categories;
  }

  public EventQuery setCategories(String[] categories) {
    this.categories = categories;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "resource", resourceKey);
    appendUrlParameter(url, "categories", categories);
    if (fromDate != null) {
      if (includeFromTime) {
        appendUrlParameter(url, "fromDateTime", fromDate, true);
      } else {
        appendUrlParameter(url, "fromDate", fromDate, false);
      }
    }
    if (toDate != null) {
      if (includeToTime) {
        appendUrlParameter(url, "toDateTime", toDate, true);
      } else {
        appendUrlParameter(url, "toDate", toDate, false);
      }
    }
    return url.toString();
  }

  @Override
  public Class<Event> getModelClass() {
    return Event.class;
  }
}
