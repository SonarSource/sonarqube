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

package org.sonar.server.startup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicMapper;
import org.sonar.core.technicaldebt.db.RequirementMigrationDto;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RegisterRules;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * This script copy every requirements from characteristics table (every row where rule_id is not null) to the rules table.
 * <p/>
 * This script need to be executed after rules registration because default debt columns (characteristics, function, coefficient and offset) has to be populated
 * in order to be able to compare default values with overridden values.
 * <p/>
 * WARNING : When updating this class, please take time to test on ALL databases!
 *
 * @since 4.3 this component could be removed after 4 or 5 releases.
 */
@ServerSide
public class CopyRequirementsFromCharacteristicsToRules {

  private static final Logger LOGGER = Loggers.get(CopyRequirementsFromCharacteristicsToRules.class);

  private static final String TEMPLATE_KEY = "CopyRequirementsFromCharacteristicsToRules";

  private final DbClient dbClient;

  /**
   * @param registerRules used only to be started after init of rules
   */
  public CopyRequirementsFromCharacteristicsToRules(DbClient dbClient, RegisterRules registerRules) {
    this.dbClient = dbClient;
  }

  public void start() {
    doExecute();
  }

  private void doExecute() {
    if (dbClient.loadedTemplateDao().countByTypeAndKey(LoadedTemplateDto.ONE_SHOT_TASK_TYPE, TEMPLATE_KEY) == 0) {
      LOGGER.info("Copying requirement from characteristics to rules");
      copyRequirementsFromCharacteristicsToRules();

      LOGGER.info("Deleting requirements from characteristics");
      removeRequirementsDataFromCharacteristics();

      dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(TEMPLATE_KEY, LoadedTemplateDto.ONE_SHOT_TASK_TYPE));
    }
  }

  private void copyRequirementsFromCharacteristicsToRules() {
    DbSession dbSession = dbClient.openSession(true);

    try {
      List<RequirementMigrationDto> requirementDtos = dbSession.getMapper(CharacteristicMapper.class).selectDeprecatedRequirements();
      if (requirementDtos.isEmpty()) {
        LOGGER.info("No requirement need to be copied", requirementDtos);

      } else {
        int requirementCopied = 0;

        final Multimap<Integer, RequirementMigrationDto> requirementsByRuleId = ArrayListMultimap.create();
        for (RequirementMigrationDto requirementDto : requirementDtos) {
          requirementsByRuleId.put(requirementDto.getRuleId(), requirementDto);
        }

        List<RuleDto> rules = dbClient.ruleDao().findAll(dbSession);
        for (RuleDto rule : rules) {
          Collection<RequirementMigrationDto> requirementsForRule = requirementsByRuleId.get(rule.getId());
          if (!requirementsForRule.isEmpty()) {
            convert(rule, requirementsForRule, dbSession);
            requirementCopied++;
          }
        }
        dbSession.commit();

        LOGGER.info("{} requirements have been found, {} have been copied", requirementDtos.size(), requirementCopied);
      }
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void convert(RuleDto rule, Collection<RequirementMigrationDto> requirementsForRule, DbSession session) {
    RequirementMigrationDto enabledRequirement = enabledRequirement(requirementsForRule);

    if (enabledRequirement == null && RuleStatus.REMOVED != rule.getStatus()) {
      // If no enabled requirement is found, it means that the requirement has been disabled for this rule
      convertDisableRequirement(rule, session);

    } else if (enabledRequirement != null) {
      // If one requirement is enable, it means either that this requirement has been set from SQALE, or that it come from a XML model
      // definition
      convertEnabledRequirement(rule, enabledRequirement, session);

      // When default values on debt are the same that ones set by SQALE, nothing to do
    }
  }

  private static RequirementMigrationDto enabledRequirement(Collection<RequirementMigrationDto> requirementsForRule) {
    return Iterables.find(requirementsForRule, new Predicate<RequirementMigrationDto>() {
      @Override
      public boolean apply(@Nullable RequirementMigrationDto input) {
        return input != null && input.isEnabled();
      }
    }, null);
  }

  private void convertDisableRequirement(RuleDto rule, DbSession session) {
    rule.setSubCharacteristicId(RuleDto.DISABLED_CHARACTERISTIC_ID);
    rule.setRemediationFunction(null);
    rule.setRemediationCoefficient(null);
    rule.setRemediationOffset(null);
    dbClient.ruleDao().update(session, rule);
  }

  private void convertEnabledRequirement(RuleDto ruleRow, RequirementMigrationDto enabledRequirement, DbSession session) {
    ruleRow.setSubCharacteristicId(enabledRequirement.getParentId() != null ? enabledRequirement.getParentId() : null);
    ruleRow.setRemediationFunction(enabledRequirement.getFunction().toUpperCase());
    ruleRow.setRemediationCoefficient(convertDuration(enabledRequirement.getCoefficientValue(), enabledRequirement.getCoefficientUnit()));
    ruleRow.setRemediationOffset(convertDuration(enabledRequirement.getOffsetValue(), enabledRequirement.getOffsetUnit()));

    // Constant/issue with coefficient is replaced by Constant/issue with offset (with no coefficient)
    if (DebtRemediationFunction.Type.CONSTANT_ISSUE.name().equals(ruleRow.getRemediationFunction())
      && ruleRow.getRemediationCoefficient() != null) {
      ruleRow.setRemediationOffset(ruleRow.getRemediationCoefficient());
      ruleRow.setRemediationCoefficient(null);
    }

    // If the coefficient of a linear or linear with offset function is null, it should be replaced by 0
    if ((DebtRemediationFunction.Type.LINEAR.name().equals(ruleRow.getRemediationFunction()) ||
      DebtRemediationFunction.Type.LINEAR_OFFSET.name().equals(ruleRow.getRemediationFunction()))
      && ruleRow.getRemediationCoefficient() == null) {
      ruleRow.setRemediationCoefficient("0" + convertUnit(enabledRequirement.getCoefficientUnit()));
      // If the offset of a constant per issue or linear with offset function is null, it should be replaced by 0
    } else if ((DebtRemediationFunction.Type.CONSTANT_ISSUE.name().equals(ruleRow.getRemediationFunction())
      || DebtRemediationFunction.Type.LINEAR_OFFSET.name().equals(ruleRow.getRemediationFunction()))
      && ruleRow.getRemediationOffset() == null) {
      ruleRow.setRemediationOffset("0" + convertUnit(enabledRequirement.getOffsetUnit()));
    }

    if (!isDebtDefaultValuesSameAsOverriddenValues(ruleRow)) {
      // Default values on debt are not the same that ones set by SQALE, update the rule
      dbClient.ruleDao().update(session, ruleRow);
    }
  }

  @CheckForNull
  @VisibleForTesting
  static String convertDuration(@Nullable Double oldValue, @Nullable String oldUnit) {
    if (oldValue != null && oldValue > 0) {
      // As value is stored in double, we have to round it in order to have an integer (for instance, if it was 1.6, we'll use 2)
      return Integer.toString((int) Math.round(oldValue)) + convertUnit(oldUnit);
    }
    return null;
  }

  @VisibleForTesting
  private static String convertUnit(@Nullable String oldUnit) {
    String unit = oldUnit != null ? oldUnit : Duration.DAY;
    return "mn".equals(unit) ? Duration.MINUTE : unit;
  }

  @VisibleForTesting
  static boolean isDebtDefaultValuesSameAsOverriddenValues(RuleDto rule) {
    return new EqualsBuilder()
      .append(rule.getDefaultSubCharacteristicId(), rule.getSubCharacteristicId())
      .append(rule.getDefaultRemediationFunction(), rule.getRemediationFunction())
      .append(rule.getDefaultRemediationCoefficient(), rule.getRemediationCoefficient())
      .append(rule.getDefaultRemediationOffset(), rule.getRemediationOffset())
      .isEquals();
  }

  private void removeRequirementsDataFromCharacteristics() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      dbSession.getMapper(CharacteristicMapper.class).deleteRequirementsFromCharacteristicsTable();
      dbSession.commit();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

}
