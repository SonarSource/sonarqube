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

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceQueryTest extends QueryTestCase {

  @Test
  public void resource() {
    ResourceQuery query = new ResourceQuery("org.foo:bar");
    assertThat(query.getUrl()).isEqualTo(("/api/resources?resource=org.foo%3Abar&verbose=false&"));
    assertThat(query.getResourceKeyOrId()).isEqualTo(("org.foo:bar"));
    assertThat(query.isVerbose()).isEqualTo((false));
  }

  @Test
  public void measures() {
    ResourceQuery query = new ResourceQuery();
    query.setMetrics("loc", "coverage", "lines");
    assertThat(query.getUrl()).isEqualTo(("/api/resources?metrics=loc,coverage,lines&verbose=false&"));
    assertThat(query.getResourceKeyOrId()).isNull();
    assertThat(query.getMetrics()).isEqualTo((new String[]{"loc", "coverage", "lines"}));
  }

  @Test
  public void measuresWithTrends() {
    ResourceQuery query = new ResourceQuery();
    query.setIncludeTrends(true);

    assertThat(query.getUrl()).isEqualTo(("/api/resources?includetrends=true&verbose=false&"));
  }

  @Test
  public void measuresWithAlerts() {
    ResourceQuery query = new ResourceQuery();
    query.setIncludeAlerts(true);

    assertThat(query.getUrl()).isEqualTo(("/api/resources?includealerts=true&verbose=false&"));
  }

  @Test
  public void measuresOnRules() {
    ResourceQuery query = new ResourceQuery().setMetrics("violations");
    query.setRules("ruleA", "ruleB");
    assertThat(query.getUrl()).isEqualTo(("/api/resources?metrics=violations&rules=ruleA,ruleB&verbose=false&"));
  }

  @Test
  public void measuresOnRulePriorities() {
    ResourceQuery query = new ResourceQuery().setMetrics("violations");
    query.setRuleSeverities("MAJOR", "MINOR");

    assertThat(query.getUrl()).isEqualTo(("/api/resources?metrics=violations&rule_priorities=MAJOR,MINOR&verbose=false&"));
  }

  @Test
  public void should_create_query() {
    ResourceQuery query = ResourceQuery.createForMetrics("org.foo", "ncloc", "lines");
    assertThat(query.getResourceKeyOrId()).isEqualTo(("org.foo"));
    assertThat(query.getMetrics()).isEqualTo((new String[]{"ncloc", "lines"}));
  }

  @Test
  public void should_create_query_from_resource() {
    ResourceQuery query = ResourceQuery.createForResource(new Resource().setId(1), "ncloc");
    assertThat(query.getResourceKeyOrId()).isEqualTo("1");
    assertThat(query.getUrl()).isEqualTo("/api/resources?resource=1&metrics=ncloc&verbose=false&");
  }

  @Test
  public void should_not_create_query_from_resource_without_id() {
    try {
      ResourceQuery.createForResource(new Resource());
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
