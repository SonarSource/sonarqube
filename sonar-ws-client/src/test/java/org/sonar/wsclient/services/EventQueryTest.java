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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public final class EventQueryTest extends QueryTestCase {

  @Test
  public void from() {
    EventQuery query = new EventQuery();
    query.setResourceKey("key");
    Date from = new Date();
    query.setFrom(from, true);
    assertEquals(from, query.getFrom());
    assertEquals(true, query.isIncludeFromTime());
  }

  @Test
  public void to() {
    EventQuery query = new EventQuery();
    query.setResourceKey("key");
    Date to = new Date();
    query.setTo(to, true);
    assertEquals(to, query.getTo());
    assertEquals(true, query.isIncludeToTime());
  }

  @Test
  public void urlWithTime() {
    EventQuery query = new EventQuery("key");
    query.setCategories(new String[] { "category" });
    Date date = new Date();
    query.setTo(date, true);
    query.setFrom(date, true);

    assertNotNull(query.getResourceKey());
    assertNotNull(query.getCategories());

    String url = query.getUrl();
    assertNotNull(url);
    assertTrue(url.contains("fromDateTime"));
    assertTrue(url.contains("toDateTime"));
  }

  @Test
  public void urlWithoutTime() {
    EventQuery query = new EventQuery("key");
    query.setCategories(new String[] { "category" });
    Date date = new Date();
    query.setTo(date, false);
    query.setFrom(date, false);

    final String url = query.getUrl();
    assertNotNull(url);
    assertFalse(url.contains("fromDateTime"));
    assertFalse(url.contains("toDateTime"));
    assertTrue(url.contains("fromDate"));
    assertTrue(url.contains("toDate"));
  }

  @Test
  public void urlWithoutDate() {
    EventQuery query = new EventQuery("key");
    query.setCategories(new String[] { "category" });

    final String url = query.getUrl();
    assertNotNull(url);
    assertFalse(url.contains("fromDateTime"));
    assertFalse(url.contains("toDateTime"));
    assertFalse(url.contains("fromDate"));
    assertFalse(url.contains("toDate"));
  }

}
