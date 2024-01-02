/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.impl.server.RulesDefinitionContext;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.RuleStatus.DEPRECATED;

public class CommonRuleDefinitionsImplTest {

  @Test
  public void instantiate_common_rules_for_correct_languages_only() {
    CommonRuleDefinitionsImpl commonRuleDefinitions = new CommonRuleDefinitionsImpl(getLanguages());
    RulesDefinition.Context underTest = new RulesDefinitionContext();
    commonRuleDefinitions.define(underTest);

    assertThat(underTest.repositories()).hasSize(3);
    assertThat(underTest.repository("common-java")).isNotNull();
    assertThat(underTest.repository("common-php")).isNotNull();
    assertThat(underTest.repository("common-js")).isNotNull();

    for (RulesDefinition.Repository repository : underTest.repositories()) {
      assertThat(repository.rules()).hasSize(6);
      for (RulesDefinition.Rule rule : repository.rules()) {
        assertThat(rule.status()).isEqualTo(DEPRECATED);
      }
    }
  }

  private Languages getLanguages() {
    return new Languages(
      createLanguage("java"),
      createLanguage("php"),
      createLanguage("js"),
      createLanguage("terraform"),
      createLanguage("cloudformation"),
      createLanguage("kubernetes"),
      createLanguage("docker"),
      createLanguage("web"),
      createLanguage("css"),
      createLanguage("xml"),
      createLanguage("yaml"),
      createLanguage("json"),
      createLanguage("jsp"),
      createLanguage("text"),
      createLanguage("secrets")
    );
  }

  private Language createLanguage(String languageKey) {
    return new AbstractLanguage(languageKey, languageKey + "_name") {
      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }
    };
  }

}
