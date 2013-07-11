/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import static org.fest.assertions.Assertions.assertThat;

public class ViolationQueryTest extends QueryTestCase {

  @Test
  public void should_create_query() {
    ViolationQuery query = ViolationQuery.createForResource("myproject:org.foo:bar");
    assertThat(query.getUrl()).isEqualTo("/api/violations?resource=myproject%3Aorg.foo%3Abar&");
    assertThat(query.getModelClass().getName()).isEqualTo(Violation.class.getName());
  }

  @Test
  public void should_create_query_tree() {
    ViolationQuery query = ViolationQuery.createForResource("myproject")
      .setDepth(-1)
      .setLimit(20)
      .setSeverities("MAJOR", "BLOCKER")
      .setQualifiers("FIL")
      .setRuleKeys("checkstyle:foo", "pmd:bar");
    assertThat(query.getUrl()).isEqualTo(
      "/api/violations?resource=myproject&depth=-1&limit=20&qualifiers=FIL&rules=checkstyle%3Afoo,pmd%3Abar&priorities=MAJOR,BLOCKER&");
  }

  @Test
  public void should_create_query_from_resource() {
    ViolationQuery query = ViolationQuery.createForResource(new Resource().setId(1));
    assertThat(query.getUrl()).isEqualTo("/api/violations?resource=1&");
    assertThat(query.getModelClass().getName()).isEqualTo(Violation.class.getName());
  }

  @Test
  public void should_not_create_query_from_resource_without_id() {
    try {
      ViolationQuery.createForResource(new Resource());
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
