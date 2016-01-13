/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleRepositoriesTest {

  @Test
  public void should_register_repositories() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new SquidDefinitions().define(context);
    new FindbugsDefinitions().define(context);

    RuleRepositories repositories = new RuleRepositories();
    repositories.register(context);

    RuleRepositories.Repository findbugs = repositories.repository("findbugs");
    assertThat(findbugs).isNotNull();
    assertThat(findbugs.key()).isEqualTo("findbugs");
    assertThat(findbugs.name()).isEqualTo("Findbugs");
    assertThat(findbugs.language()).isEqualTo("java");

    // for backward-compatibility
    assertThat(findbugs.getKey()).isEqualTo("findbugs");
    assertThat(findbugs.getName(false)).isEqualTo("Findbugs");
    assertThat(findbugs.getName(true)).isEqualTo("Findbugs");
    assertThat(findbugs.getName()).isEqualTo("Findbugs");
    assertThat(findbugs.getLanguage()).isEqualTo("java");

    RuleRepositories.Repository squid = repositories.repository("squid");
    assertThat(squid).isNotNull();
    assertThat(squid.key()).isEqualTo("squid");
    assertThat(squid.name()).isEqualTo("Squid");
    assertThat(squid.language()).isEqualTo("java");

    assertThat(repositories.repositories()).containsOnly(findbugs, squid);
    assertThat(repositories.repositoriesForLang("java")).containsOnly(findbugs, squid);

    // test equals() and hashCode()
    assertThat(findbugs).isEqualTo(findbugs).isNotEqualTo(squid).isNotEqualTo("findbugs").isNotEqualTo(null);
  }

  @Test
  public void register_repositories_having_same_name() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new RulesDefinition() {
      @Override
      public void define(Context context) {
        context.createRepository("squid", "java").setName("SonarQube").done();
      }
    }.define(context);

    // Repository with same name
    new RulesDefinition() {
      @Override
      public void define(Context context) {
        context.createRepository("javascript", "js").setName("SonarQube").done();
      }
    }.define(context);

    RuleRepositories repositories = new RuleRepositories();
    repositories.register(context);

    assertThat(repositories.repositories()).hasSize(2);
  }

  static class FindbugsDefinitions implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("findbugs", "java");
      repo.setName("Findbugs");
      repo.createRule("ABC")
        .setName("ABC")
        .setHtmlDescription("Description of ABC");
      repo.done();
    }
  }

  static class SquidDefinitions implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("squid", "java");
      repo.setName("Squid");
      repo.createRule("DEF")
        .setName("DEF")
        .setHtmlDescription("Description of DEF");
      repo.done();
    }
  }
}
