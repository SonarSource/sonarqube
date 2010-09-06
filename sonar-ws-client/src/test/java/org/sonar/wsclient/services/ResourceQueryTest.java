/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.wsclient.services;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ResourceQueryTest {

  @Test
  public void resource() {
    ResourceQuery query = new ResourceQuery("org.foo:bar");
    assertThat(query.getUrl(), is("/api/resources?resource=org.foo:bar&verbose=false&"));
    assertThat(query.getResourceKeyOrId(), is("org.foo:bar"));
    assertThat(query.isVerbose(), is(false));
  }

  @Test
  public void measures() {
    ResourceQuery query = new ResourceQuery();
    query.setMetrics("loc", "coverage", "lines");
    assertThat(query.getUrl(), is("/api/resources?metrics=loc,coverage,lines&verbose=false&"));
    assertThat(query.getResourceKeyOrId(), nullValue());
    assertThat(query.getMetrics(), is(new String[]{"loc", "coverage", "lines"}));
  }

  @Test
  public void measuresWithTrends() {
    ResourceQuery query = new ResourceQuery();
    query.setIncludeTrends(true);

    assertThat(query.getUrl(), is("/api/resources?includetrends=true&verbose=false&"));
  }

  @Test
  public void measuresOnRules() {
    ResourceQuery query = new ResourceQuery().setMetrics("violations");
    query.setRules("ruleA", "ruleB");
    assertThat(query.getUrl(), is("/api/resources?metrics=violations&rules=ruleA,ruleB&verbose=false&"));
  }

  @Test
  public void measuresOnRulePriorities() {
    ResourceQuery query = new ResourceQuery().setMetrics("violations");
    query.setRulePriorities("MAJOR,MINOR");

    assertThat(query.getUrl(), is("/api/resources?metrics=violations&rule_priorities=MAJOR,MINOR&verbose=false&"));
  }

  @Test
  public void build() {
    ResourceQuery query = ResourceQuery.createForMetrics("org.foo", "ncloc", "lines");
    assertThat(query.getResourceKeyOrId(), is("org.foo"));
    assertThat(query.getMetrics(), is(new String[]{"ncloc", "lines"}));
  }
}
