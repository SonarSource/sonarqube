/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.qa.bluegreen;

import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public class RulesDefinitionV1 implements RulesDefinition {

  public static final String REPOSITORY_KEY = "bluegreen";

  @Override
  public void define(Context context) {
    NewRepository repo = context.createRepository(REPOSITORY_KEY, "xoo").setName("BlueGreen");
    repo.createRule("a")
      .setName("Rule A")
      .setHtmlDescription("Rule A")
      .setType(RuleType.VULNERABILITY);
    NewRule ruleB = repo.createRule("b")
      .setName("Rule B")
      .setHtmlDescription("Rule B")
      .setType(RuleType.VULNERABILITY);
    ruleB.createParam("p1").setName("Param One");
    ruleB.createParam("p2").setName("Param Two");
    repo.done();
  }
}
