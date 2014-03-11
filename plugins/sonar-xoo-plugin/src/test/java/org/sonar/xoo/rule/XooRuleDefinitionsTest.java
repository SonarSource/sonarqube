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
package org.sonar.xoo.rule;

import org.junit.Test;
import org.sonar.api.server.rule.RuleDefinitions;

import static org.fest.assertions.Assertions.assertThat;

public class XooRuleDefinitionsTest {

  @Test
  public void define_xoo_rules() {
    XooRuleDefinitions def = new XooRuleDefinitions();
    RuleDefinitions.Context context = new RuleDefinitions.Context();
    def.define(context);

    RuleDefinitions.Repository repo = context.repository("xoo");
    assertThat(repo).isNotNull();
    assertThat(repo.name()).isEqualTo("Xoo");
    assertThat(repo.language()).isEqualTo("xoo");
    assertThat(repo.rules()).hasSize(1);

    RuleDefinitions.Rule x1 = repo.rule("x1");
    assertThat(x1.key()).isEqualTo("x1");
    assertThat(x1.tags()).containsOnly("style", "security");
    assertThat(x1.htmlDescription()).isNotEmpty();
  }
}
