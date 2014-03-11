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

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResourceSearchQueryTest extends QueryTestCase {

  @Test
  public void test_url() {
    ResourceSearchQuery query = ResourceSearchQuery.create("commons");
    assertThat(query.getUrl(), is("/api/resources/search?s=commons&"));
    assertThat(query.getModelClass().getName(), is(ResourceSearchResult.class.getName()));
  }

  @Test
  public void test_encode_url_search_param() {
    ResourceSearchQuery query = ResourceSearchQuery.create("commons logging");
    assertThat(query.getUrl(), is("/api/resources/search?s=commons+logging&"));
    assertThat(query.getModelClass().getName(), is(ResourceSearchResult.class.getName()));
  }

  @Test
  public void test_optional_parameters() {
    ResourceSearchQuery query = ResourceSearchQuery.create("commons");
    query.setPage(5);
    query.setPageSize(20);
    query.setQualifiers("TRK", "BRC");
    assertThat(query.getUrl(), is("/api/resources/search?s=commons&p=5&ps=20&q=TRK,BRC&"));
  }
}
