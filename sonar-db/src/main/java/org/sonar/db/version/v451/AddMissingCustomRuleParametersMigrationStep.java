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
package org.sonar.db.version.v451;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.ProgressLogger;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.version.MigrationStep;
import org.sonar.db.version.v45.Migration45Mapper;
import org.sonar.db.version.v45.Rule;
import org.sonar.db.version.v45.RuleParameter;

/**
 * See http://jira.sonarsource.com/browse/SONAR-5575
 *
 * Add missing parameters (with no value) on each custom rules
 *
 * @since 4.5.1
 */
public class AddMissingCustomRuleParametersMigrationStep implements MigrationStep {

  private final DbClient db;
  private final System2 system;
  private final AtomicLong counter = new AtomicLong(0L);

  public AddMissingCustomRuleParametersMigrationStep(DbClient db, System2 system) {
    this.db = db;
    this.system = system;
  }

  @Override
  public void execute() {
    ProgressLogger progress = ProgressLogger.create(getClass(), counter);
    progress.start();

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
          insertCustomRuleParameterIfNotAlreadyExisting(templateRuleParam, templateRuleId, customRuleIdsByTemplateRuleId, customRuleParamsByRuleId, session);
        }
      }

      session.commit();

      // log the total number of process rows
      progress.log();
    } finally {
      session.close();
      progress.stop();
    }
  }

  private void insertCustomRuleParameterIfNotAlreadyExisting(RuleParameter templateRuleParam, Integer templateRuleId,
    Multimap<Integer, Integer> customRuleIdsByTemplateRuleId,
    Multimap<Integer, RuleParameter> customRuleParamsByRuleId,
    DbSession session) {
    for (Integer customRuleId : customRuleIdsByTemplateRuleId.get(templateRuleId)) {
      if (!hasParameter(templateRuleParam.getName(), customRuleParamsByRuleId.get(customRuleId))) {
        // Insert new custom rule parameter
        session.getMapper(Migration45Mapper.class).insertRuleParameter(new RuleParameter()
          .setRuleId(customRuleId)
          .setRuleTemplateId(templateRuleId)
          .setName(templateRuleParam.getName())
          .setDescription(templateRuleParam.getDescription())
          .setType(templateRuleParam.getType())
          );

        // Update updated at date of custom rule in order to allow E/S indexation
        session.getMapper(Migration45Mapper.class).updateRuleUpdateAt(customRuleId, new Date(system.now()));

        counter.getAndIncrement();
      }
    }
  }

  private static boolean hasParameter(String parameter, Collection<RuleParameter> customRuleParams) {
    return Iterables.any(customRuleParams, new MatchParameter(parameter));
  }

  private static class MatchParameter implements Predicate<RuleParameter> {
    private final String parameter;

    public MatchParameter(String parameter) {
      this.parameter = parameter;
    }

    @Override
    public boolean apply(@Nullable RuleParameter input) {
      return input != null && input.getName().equals(parameter);
    }
  }
}
