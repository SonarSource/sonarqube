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

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ViolationQueryTest {

  @Test
  public void resourceViolations() {
    ViolationQuery query = ViolationQuery.createForResource("myproject:org.foo:bar");
    assertThat(query.getUrl(), is("/api/violations?resource=myproject:org.foo:bar&"));
    assertThat(query.getModelClass().getName(), Is.is(Violation.class.getName()));
  }

  @Test
  public void resourceTreeViolations() {
    ViolationQuery query = ViolationQuery.createForResource("myproject")
        .setDepth(-1)
        .setPriorities("MAJOR", "BLOCKER")
        .setQualifiers("FIL")
        .setRuleKeys("checkstyle:foo", "pmd:bar");
    assertThat(query.getUrl(), is("/api/violations?resource=myproject&depth=-1&qualifiers=FIL&rules=checkstyle:foo,pmd:bar&priorities=MAJOR,BLOCKER&"));
  }
}
