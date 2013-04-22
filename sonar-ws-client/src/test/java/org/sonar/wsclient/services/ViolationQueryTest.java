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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ViolationQueryTest extends QueryTestCase {

  @Test
  public void resourceViolations() {
    ViolationQuery query = ViolationQuery.createForResource("myproject:org.foo:bar");
    assertThat(query.getUrl(), is("/api/violations?resource=myproject%3Aorg.foo%3Abar&"));
    assertThat(query.getModelClass().getName(), is(Violation.class.getName()));
  }

  @Test
  public void resourceTreeViolations() {
    ViolationQuery query = ViolationQuery.createForResource("myproject")
        .setDepth(-1)
        .setLimit(20)
        .setSeverities("MAJOR", "BLOCKER")
        .setQualifiers("FIL")
        .setRuleKeys("checkstyle:foo", "pmd:bar")
        .setOutput("html");
    assertThat(
        query.getUrl(),
        is("/api/violations?resource=myproject&depth=-1&limit=20&qualifiers=FIL&rules=checkstyle%3Afoo,pmd%3Abar&priorities=MAJOR,BLOCKER&output=html&"));
  }
}
