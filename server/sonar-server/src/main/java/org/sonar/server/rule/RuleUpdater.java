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
package org.sonar.server.rule;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class RuleUpdater {

  private final DbClient dbClient;
  private final System2 system;

  public RuleUpdater(DbClient dbClient, System2 system) {
    this.dbClient = dbClient;
    this.system = system;
  }

  /**
   * Update manual rules and custom rules (rules instantiated from templates)
   */
  public boolean update(RuleUpdate update, UserSession userSession) {
    if (update.isEmpty()) {
      return false;
    }

    DbSession dbSession = dbClient.openSession(false);
    try {
      Context context = newContext(update);
      // validate only the changes, not all the rule fields
      apply(update, context, userSession);
      dbClient.ruleDao().update(dbSession, context.rule);
      updateParameters(dbSession, update, context);
      dbSession.commit();
      return true;

    } finally {
      dbSession.close();
    }
  }

  /**
   * Load all the DTOs required for validating changes and updating rule
   */
  private Context newContext(RuleUpdate change) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Context context = new Context();
      context.rule = dbClient.ruleDao().getByKey(dbSession, change.getRuleKey());
      if (RuleStatus.REMOVED == context.rule.getStatus()) {
        throw new IllegalArgumentException("Rule with REMOVED status cannot be updated: " + change.getRuleKey());
      }
      String subCharacteristicKey = change.getDebtSubCharacteristicKey();
      if (subCharacteristicKey != null &&
        !subCharacteristicKey.equals(RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC)) {
        CharacteristicDto characteristicDto = dbClient.debtCharacteristicDao().selectByKey(subCharacteristicKey, dbSession);
        if (characteristicDto == null) {
          throw new IllegalArgumentException("Unknown debt sub-characteristic: " + subCharacteristicKey);
        }
        if (!characteristicDto.isEnabled()) {
          throw new IllegalArgumentException("Debt sub-characteristic is disabled: " + subCharacteristicKey);
        }
        if (characteristicDto.getParentId() == null) {
          throw new IllegalArgumentException("Not a sub-characteristic: " + subCharacteristicKey);
        }
        context.newCharacteristic = characteristicDto;
      }
      return context;

    } finally {
      dbSession.close();
    }
  }

  private void apply(RuleUpdate update, Context context, UserSession userSession) {
    if (update.isChangeName()) {
      updateName(update, context);
    }
    if (update.isChangeDescription()) {
      updateDescription(update, context);
    }
    if (update.isChangeSeverity()) {
      updateSeverity(update, context);
    }
    if (update.isChangeStatus()) {
      updateStatus(update, context);
    }
    if (update.isChangeMarkdownNote()) {
      updateMarkdownNote(update, context, userSession);
    }
    if (update.isChangeTags()) {
      updateTags(update, context);
    }
    if (update.isChangeDebtSubCharacteristic()) {
      updateDebtSubCharacteristic(update, context);
    }
    // order is important -> sub-characteristic must be set
    if (update.isChangeDebtRemediationFunction()) {
      updateDebtRemediationFunction(update, context);
    }
  }

  private void updateName(RuleUpdate update, Context context) {
    String name = update.getName();
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("The name is missing");
    } else {
      context.rule.setName(name);
    }
  }

  private void updateDescription(RuleUpdate update, Context context) {
    String description = update.getMarkdownDescription();
    if (Strings.isNullOrEmpty(description)) {
      throw new IllegalArgumentException("The description is missing");
    } else {
      context.rule.setDescription(description);
    }
  }

  private void updateSeverity(RuleUpdate update, Context context) {
    String severity = update.getSeverity();
    if (Strings.isNullOrEmpty(severity) || !Severity.ALL.contains(severity)) {
      throw new IllegalArgumentException("The severity is invalid");
    } else {
      context.rule.setSeverity(severity);
    }
  }

  private void updateStatus(RuleUpdate update, Context context) {
    RuleStatus status = update.getStatus();
    if (status == null) {
      throw new IllegalArgumentException("The status is missing");
    } else {
      context.rule.setStatus(status);
    }
  }

  private void updateTags(RuleUpdate update, Context context) {
    Set<String> tags = update.getTags();
    if (tags == null || tags.isEmpty()) {
      context.rule.setTags(Collections.<String>emptySet());
    } else {
      RuleTagHelper.applyTags(context.rule, tags);
    }
  }

  private void updateDebtSubCharacteristic(RuleUpdate update, Context context) {
    if (update.getDebtSubCharacteristicKey() == null) {
      // set to "none"
      Integer id = context.rule.getDefaultSubCharacteristicId() != null ? RuleDto.DISABLED_CHARACTERISTIC_ID : null;
      context.rule.setSubCharacteristicId(id);
      context.rule.setRemediationFunction(null);
      context.rule.setRemediationCoefficient(null);
      context.rule.setRemediationOffset(null);

    } else if (StringUtils.equals(update.getDebtSubCharacteristicKey(), RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC)) {
      // reset to default
      context.rule.setSubCharacteristicId(null);
      context.rule.setRemediationFunction(null);
      context.rule.setRemediationCoefficient(null);
      context.rule.setRemediationOffset(null);

    } else {
      if (ObjectUtils.equals(context.newCharacteristic.getId(), context.rule.getDefaultSubCharacteristicId())) {
        // reset to default -> compatibility with SQALE
        context.rule.setSubCharacteristicId(null);
        context.rule.setRemediationFunction(null);
        context.rule.setRemediationCoefficient(null);
        context.rule.setRemediationOffset(null);
      } else {
        // override default
        context.rule.setSubCharacteristicId(context.newCharacteristic.getId());
      }
    }
  }

  private void updateDebtRemediationFunction(RuleUpdate update, Context context) {
    Integer subChar = context.rule.getSubCharacteristicId();
    boolean noChar =
      (context.rule.getDefaultSubCharacteristicId() == null && subChar == null) ||
        (subChar != null && subChar.intValue() == RuleDto.DISABLED_CHARACTERISTIC_ID);

    DebtRemediationFunction function = update.getDebtRemediationFunction();
    if (noChar || function == null) {
      context.rule.setRemediationFunction(null);
      context.rule.setRemediationCoefficient(null);
      context.rule.setRemediationOffset(null);
    } else {
      if (isSameAsDefaultFunction(function, context.rule)) {
        // reset to default
        context.rule.setRemediationFunction(null);
        context.rule.setRemediationCoefficient(null);
        context.rule.setRemediationOffset(null);
      } else {
        context.rule.setRemediationFunction(function.type().name());
        context.rule.setRemediationCoefficient(function.coefficient());
        context.rule.setRemediationOffset(function.offset());
      }
    }
  }

  private void updateMarkdownNote(RuleUpdate update, Context context, UserSession userSession) {
    if (StringUtils.isBlank(update.getMarkdownNote())) {
      context.rule.setNoteData(null);
      context.rule.setNoteCreatedAt(null);
      context.rule.setNoteUpdatedAt(null);
      context.rule.setNoteUserLogin(null);
    } else {
      Date now = new Date(system.now());
      context.rule.setNoteData(update.getMarkdownNote());
      context.rule.setNoteCreatedAt(context.rule.getNoteCreatedAt() != null ? context.rule.getNoteCreatedAt() : now);
      context.rule.setNoteUpdatedAt(now);
      context.rule.setNoteUserLogin(userSession.login());
    }
  }

  private static boolean isSameAsDefaultFunction(DebtRemediationFunction fn, RuleDto rule) {
    return new EqualsBuilder()
      .append(fn.type().name(), rule.getDefaultRemediationFunction())
      .append(fn.coefficient(), rule.getDefaultRemediationCoefficient())
      .append(fn.offset(), rule.getDefaultRemediationOffset())
      .isEquals();
  }

  private void updateParameters(DbSession dbSession, RuleUpdate update, Context context) {
    if (update.isChangeParameters() && update.isCustomRule()) {
      RuleDto customRule = context.rule;
      RuleDto templateRule = dbClient.ruleDao().getTemplate(customRule, dbSession);
      if (templateRule == null) {
        throw new IllegalStateException(String.format("Template %s of rule %s does not exist",
          customRule.getTemplateId(), customRule.getKey()));
      }
      List<String> paramKeys = newArrayList();

      // Load active rules and its parameters in cache
      Multimap<RuleDto, ActiveRuleDto> activeRules = ArrayListMultimap.create();
      Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParams = ArrayListMultimap.create();
      for (ActiveRuleDto activeRuleDto : dbClient.activeRuleDao().findByRule(dbSession, customRule)) {
        activeRules.put(customRule, activeRuleDto);
        for (ActiveRuleParamDto activeRuleParamDto : dbClient.activeRuleDao().findParamsByActiveRuleKey(dbSession, activeRuleDto.getKey())) {
          activeRuleParams.put(activeRuleDto, activeRuleParamDto);
        }
      }

      // Browse custom rule parameters to create, update or delete them
      deleteOrUpdateParameters(dbSession, update, customRule, paramKeys, activeRules, activeRuleParams);
    }
  }

  private void deleteOrUpdateParameters(DbSession dbSession, RuleUpdate update, RuleDto customRule, List<String> paramKeys,
    Multimap<RuleDto, ActiveRuleDto> activeRules, Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParams) {
    for (RuleParamDto ruleParamDto : dbClient.ruleDao().findRuleParamsByRuleKey(dbSession, update.getRuleKey())) {
      String key = ruleParamDto.getName();
      String value = Strings.emptyToNull(update.parameter(key));

      // Update rule param
      ruleParamDto.setDefaultValue(value);
      dbClient.ruleDao().updateRuleParam(dbSession, customRule, ruleParamDto);

      if (value != null) {
        // Update linked active rule params or create new one
        for (ActiveRuleDto activeRuleDto : activeRules.get(customRule)) {
          for (ActiveRuleParamDto activeRuleParamDto : activeRuleParams.get(activeRuleDto)) {
            if (activeRuleParamDto.getKey().equals(key)) {
              dbClient.activeRuleDao().updateParam(dbSession, activeRuleDto, activeRuleParamDto.setValue(value));
            } else {
              dbClient.activeRuleDao().addParam(dbSession, activeRuleDto, ActiveRuleParamDto.createFor(ruleParamDto).setValue(value));
            }
          }
        }
      } else {
        // Delete linked active rule params
        for (ActiveRuleDto activeRuleDto : activeRules.get(customRule)) {
          for (ActiveRuleParamDto activeRuleParamDto : activeRuleParams.get(activeRuleDto)) {
            if (activeRuleParamDto.getKey().equals(key)) {
              dbClient.activeRuleDao().deleteParam(dbSession, activeRuleDto, activeRuleParamDto);
            }
          }
        }
      }
      paramKeys.add(key);
    }
  }

  /**
   * Data loaded before update
   */
  private static class Context {
    private RuleDto rule;
    private CharacteristicDto newCharacteristic;
  }

}
