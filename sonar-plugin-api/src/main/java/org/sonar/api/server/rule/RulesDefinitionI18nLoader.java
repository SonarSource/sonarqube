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
package org.sonar.api.server.rule;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.i18n.RuleI18n;

/**
 * Loads the English bundles of rules (name, description and parameters) that are
 * deprecated since 4.2. It can be useful when loading existing XML files that
 * do not contain rule names and descriptions.
 * <br>
 * This class must be executed after declaring rules on {@link RulesDefinition.NewRepository}.
 * <br>
 * Note that localization of rules was dropped in version 4.2. All texts are English.
 *
 * @see org.sonar.api.server.rule.RulesDefinition for an example
 * @since 4.3
 */
public class RulesDefinitionI18nLoader {

  private final RuleI18n i18n;

  public RulesDefinitionI18nLoader(RuleI18n i18n) {
    this.i18n = i18n;
  }

  /**
   * Loads descriptions of rules and related rule parameters. Existing descriptions
   * are overridden if English labels exist in bundles.
   */
  public void load(RulesDefinition.NewRepository repo) {
    for (RulesDefinition.NewRule rule : repo.rules()) {
      String name = i18n.getName(repo.key(), rule.key());
      if (StringUtils.isNotBlank(name)) {
        rule.setName(name);
      }

      String desc = i18n.getDescription(repo.key(), rule.key());
      if (StringUtils.isNotBlank(desc)) {
        rule.setHtmlDescription(desc);
      }

      for (RulesDefinition.NewParam param : rule.params()) {
        String paramDesc = i18n.getParamDescription(repo.key(), rule.key(), param.key());
        if (StringUtils.isNotBlank(paramDesc)) {
          param.setDescription(paramDesc);
        }
      }
    }
  }
}
