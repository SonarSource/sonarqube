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
package org.sonar.plugins.xoo.rules;

import org.junit.Test;
import org.sonar.api.server.rule.RuleDefinitions;
import org.sonar.api.server.rule.RuleDefinitions.Repository;

import static org.fest.assertions.Assertions.assertThat;

public class XooRuleDefinitionsTest {


  @Test
  public void should_define_xoo_repository() {
    RuleDefinitions.Context context = new RuleDefinitions.Context();
    new XooRuleDefinitions().define(context);

    assertThat(context.repositories()).hasSize(1);

    Repository xooRepository = context.repositories().get(0);
    assertThat(xooRepository.key()).isEqualTo("xoo");
    assertThat(xooRepository.language()).isEqualTo("xoo");
    assertThat(xooRepository.name()).isEqualTo("Xoo");

    assertThat(xooRepository.rules()).hasSize(7);
  }
}
