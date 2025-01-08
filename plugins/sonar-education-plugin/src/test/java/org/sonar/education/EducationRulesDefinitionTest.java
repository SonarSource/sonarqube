/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.education;

import java.util.List;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class EducationRulesDefinitionTest {

  @Test
  public void define_addsRuleDefinitions() {
    EducationRulesDefinition educationRulesDefinition = new EducationRulesDefinition();

    RulesDefinition.Context context = new RulesDefinition.Context();

    educationRulesDefinition.define(context);

    List<RulesDefinition.Repository> repositories = context.repositories();
    assertThat(repositories).hasSize(1);

    RulesDefinition.Repository repository = context.repositories().get(0);
    List<RulesDefinition.Rule> rules = repository.rules();
    assertThat(rules).hasSizeGreaterThanOrEqualTo(4);
  }
}
