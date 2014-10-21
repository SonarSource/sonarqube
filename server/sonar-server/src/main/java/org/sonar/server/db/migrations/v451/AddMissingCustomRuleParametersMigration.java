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
package org.sonar.server.db.migrations.v451;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v45.Migration45Mapper;
import org.sonar.core.persistence.migration.v45.Rule;
import org.sonar.core.persistence.migration.v45.RuleParameter;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * See http://jira.codehaus.org/browse/SONAR-5575
 *
 * Add missing parameters (with no value) on each custom rules
 *
 * @since 4.5.1
 */
public class AddMissingCustomRuleParametersMigration implements DatabaseMigration {

  private final DbClient db;
  private final System2 system;

  public AddMissingCustomRuleParametersMigration(DbClient db, System2 system) {
    this.db = db;
    this.system = system;
  }

  @Override
  public void execute() {
    DbSession session = db.openSession(false);
    try {
      Migration45Mapper mapper = session.getMapper(Migration45Mapper.class);

      List<RuleParameter> templateRuleParams = mapper.selectAllTemplateRuleParameters();
      Multimap<Integer, RuleParameter> templateRuleParamsByRuleId = ArrayListMultimap.create();
      for (RuleParameter templateRuleParam : templateRuleParams) {
        templateRuleParamsByRuleId.put(templateRuleParam.getRuleId(), templateRuleParam);
      }

      List<Rule> customRules = mapper.selectAllCustomRules();
      Multimap<Integer, Integer> customRuleIdsByTemplateRuleId = HashMultimap.create();
      for (Rule customRule : customRules) {
        customRuleIdsByTemplateRuleId.put(customRule.getTemplateId(), customRule.getId());
      }

      List<RuleParameter> customRuleParams = mapper.selectAllCustomRuleParameters();
      Multimap<Integer, RuleParameter> customRuleParamsByRuleId = ArrayListMultimap.create();
      for (RuleParameter customRuleParam : customRuleParams) {
        customRuleParamsByRuleId.put(customRuleParam.getRuleId(), customRuleParam);
      }

      // For each parameters of template rules, verify that each custom rules has the parameter
      for (Integer templateRuleId : templateRuleParamsByRuleId.keySet()) {
        for (RuleParameter templateRuleParam : templateRuleParamsByRuleId.get(templateRuleId)) {
          // Each custom rule should have this parameter
          for (Integer customRuleId : customRuleIdsByTemplateRuleId.get(templateRuleId)) {
            if (!hasParameter(templateRuleParam.getName(), customRuleParamsByRuleId.get(customRuleId))) {
              // Insert new custom rule parameter
              mapper.insertRuleParameter(new RuleParameter()
                .setRuleId(customRuleId)
                .setRuleTemplateId(templateRuleId)
                .setName(templateRuleParam.getName())
                .setDescription(templateRuleParam.getDescription())
                .setType(templateRuleParam.getType())
              );

              // Update updated at date of custom rule in order to allow E/S indexation
              mapper.updateRuleUpdateAt(customRuleId, new Date(system.now()));
            }
          }
        }
      }

      session.commit();
    } finally {
      session.close();
    }
  }

  private boolean hasParameter(final String parameter, Collection<RuleParameter> customRuleParams) {
    return Iterables.any(customRuleParams, new Predicate<RuleParameter>() {
      @Override
      public boolean apply(@Nullable RuleParameter input) {
        return input != null && input.getName().equals(parameter);
      }
    });
  }
}
