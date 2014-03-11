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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RuleQueryTest extends QueryTestCase {

  @Test
  public void languageRules() {
    RuleQuery query = new RuleQuery("java");
    assertThat(query.getUrl(), is("/api/rules?language=java&"));
    assertThat(query.getModelClass().getName(), is(Rule.class.getName()));
  }

  @Test
  public void inactiveRules() {
    assertThat(new RuleQuery("java").setActive(true).getUrl(),
        is("/api/rules?language=java&status=ACTIVE&"));
    assertThat(new RuleQuery("java").setActive(false).getUrl(),
        is("/api/rules?language=java&status=INACTIVE&"));
  }

}
