/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.StringUtils.isBlank;

@ServerSide
public class RuleUpdater {

  private final DbClient dbClient;
  private final RuleIndexer ruleIndexer;
  private final System2 system;

  public RuleUpdater(DbClient dbClient, RuleIndexer ruleIndexer, System2 system) {
    this.dbClient = dbClient;
    this.ruleIndexer = ruleIndexer;
    this.system = system;
  }

  /**
   * Update manual rules and custom rules (rules instantiated from templates)
   */
  public boolean update(DbSession dbSession, RuleUpdate update, OrganizationDto organization, UserSession userSession) {
    if (update.isEmpty()) {
      return false;
    }

    RuleDto rule = getRuleDto(update);
    // validate only the changes, not all the rule fields
    apply(update, rule, userSession);
    update(dbSession, rule);
    updateParameters(dbSession, organization, update, rule);
    ruleIndexer.commitAndIndex(dbSession, rule.getId(), organization);

    return true;
  }

  /**
   * Load all the DTOs required for validating changes and updating rule
   */
  private RuleDto getRuleDto(RuleUpdate change) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      RuleDto rule = dbClient.ruleDao().selectOrFailByKey(dbSession, change.getOrganization(), change.getRuleKey());
      if (RuleStatus.REMOVED == rule.getStatus()) {
        throw new IllegalArgumentException("Rule with REMOVED status cannot be updated: " + change.getRuleKey());
      }
      return rule;
    }
  }

  private void apply(RuleUpdate update, RuleDto rule, UserSession userSession) {
    if (update.isChangeName()) {
      updateName(update, rule);
    }
    if (update.isChangeDescription()) {
      updateDescription(update, rule);
    }
    if (update.isChangeSeverity()) {
      updateSeverity(update, rule);
    }
    if (update.isChangeStatus()) {
      updateStatus(update, rule);
    }
    if (update.isChangeMarkdownNote()) {
      updateMarkdownNote(update, rule, userSession);
    }
    if (update.isChangeTags()) {
      updateTags(update, rule);
    }
    // order is important -> sub-characteristic must be set
    if (update.isChangeDebtRemediationFunction()) {
      updateDebtRemediationFunction(update, rule);
    }
  }

  private static void updateName(RuleUpdate update, RuleDto rule) {
    String name = update.getName();
    if (isNullOrEmpty(name)) {
      throw new IllegalArgumentException("The name is missing");
    }
    rule.setName(name);
  }

  private static void updateDescription(RuleUpdate update, RuleDto rule) {
    String description = update.getMarkdownDescription();
    if (isNullOrEmpty(description)) {
      throw new IllegalArgumentException("The description is missing");
    }
    rule.setDescription(description);
    rule.setDescriptionFormat(RuleDto.Format.MARKDOWN);
  }

  private static void updateSeverity(RuleUpdate update, RuleDto rule) {
    String severity = update.getSeverity();
    if (isNullOrEmpty(severity) || !Severity.ALL.contains(severity)) {
      throw new IllegalArgumentException("The severity is invalid");
    }
    rule.setSeverity(severity);
  }

  private static void updateStatus(RuleUpdate update, RuleDto rule) {
    RuleStatus status = update.getStatus();
    if (status == null) {
      throw new IllegalArgumentException("The status is missing");
    }
    rule.setStatus(status);
  }

  private static void updateTags(RuleUpdate update, RuleDto rule) {
    Set<String> tags = update.getTags();
    if (tags == null || tags.isEmpty()) {
      rule.setTags(Collections.emptySet());
    } else {
      RuleTagHelper.applyTags(rule, tags);
    }
  }

  private static void updateDebtRemediationFunction(RuleUpdate update, RuleDto rule) {
    DebtRemediationFunction function = update.getDebtRemediationFunction();
    if (function == null) {
      rule.setRemediationFunction(null);
      rule.setRemediationGapMultiplier(null);
      rule.setRemediationBaseEffort(null);
    } else {
      if (isSameAsDefaultFunction(function, rule)) {
        // reset to default
        rule.setRemediationFunction(null);
        rule.setRemediationGapMultiplier(null);
        rule.setRemediationBaseEffort(null);
      } else {
        rule.setRemediationFunction(function.type().name());
        rule.setRemediationGapMultiplier(function.gapMultiplier());
        rule.setRemediationBaseEffort(function.baseEffort());
      }
    }
  }

  private void updateMarkdownNote(RuleUpdate update, RuleDto rule, UserSession userSession) {
    if (isBlank(update.getMarkdownNote())) {
      rule.setNoteData(null);
      rule.setNoteCreatedAt(null);
      rule.setNoteUpdatedAt(null);
      rule.setNoteUserUuid(null);
    } else {
      long now = system.now();
      rule.setNoteData(update.getMarkdownNote());
      rule.setNoteCreatedAt(rule.getNoteCreatedAt() != null ? rule.getNoteCreatedAt() : now);
      rule.setNoteUpdatedAt(now);
      rule.setNoteUserUuid(userSession.getUuid());
    }
  }

  private static boolean isSameAsDefaultFunction(DebtRemediationFunction fn, RuleDto rule) {
    return new EqualsBuilder()
      .append(fn.type().name(), rule.getDefRemediationFunction())
      .append(fn.gapMultiplier(), rule.getDefRemediationGapMultiplier())
      .append(fn.baseEffort(), rule.getDefRemediationBaseEffort())
      .isEquals();
  }

  private void updateParameters(DbSession dbSession, OrganizationDto organization, RuleUpdate update, RuleDto rule) {
    if (update.isChangeParameters() && update.isCustomRule()) {
      RuleDto customRule = rule;
      Integer templateId = customRule.getTemplateId();
      checkNotNull(templateId, "Rule '%s' has no persisted template!", customRule);
      Optional<RuleDefinitionDto> templateRule = dbClient.ruleDao().selectDefinitionById(templateId, dbSession);
      if (!templateRule.isPresent()) {
        throw new IllegalStateException(String.format("Template %s of rule %s does not exist",
          customRule.getTemplateId(), customRule.getKey()));
      }
      List<String> paramKeys = newArrayList();

      // Load active rules and its parameters in cache
      Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParamsByActiveRule = getActiveRuleParamsByActiveRule(dbSession, organization, customRule);
      // Browse custom rule parameters to create, update or delete them
      deleteOrUpdateParameters(dbSession, update, customRule, paramKeys, activeRuleParamsByActiveRule);
    }
  }

  private Multimap<ActiveRuleDto, ActiveRuleParamDto> getActiveRuleParamsByActiveRule(DbSession dbSession, OrganizationDto organization, RuleDto customRule) {
    List<OrgActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByRuleId(dbSession, organization, customRule.getId());
    Map<Integer, OrgActiveRuleDto> activeRuleById = from(activeRuleDtos).uniqueIndex(ActiveRuleDto::getId);
    List<Integer> activeRuleIds = Lists.transform(activeRuleDtos, ActiveRuleDto::getId);
    List<ActiveRuleParamDto> activeRuleParamDtos = dbClient.activeRuleDao().selectParamsByActiveRuleIds(dbSession, activeRuleIds);
    return from(activeRuleParamDtos)
      .index(new ActiveRuleParamToActiveRule(activeRuleById));
  }

  private void deleteOrUpdateParameters(DbSession dbSession, RuleUpdate update, RuleDto customRule, List<String> paramKeys,
    Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParamsByActiveRule) {
    for (RuleParamDto ruleParamDto : dbClient.ruleDao().selectRuleParamsByRuleKey(dbSession, update.getRuleKey())) {
      String key = ruleParamDto.getName();
      String value = Strings.emptyToNull(update.parameter(key));

      // Update rule param
      ruleParamDto.setDefaultValue(value);
      dbClient.ruleDao().updateRuleParam(dbSession, customRule.getDefinition(), ruleParamDto);

      if (value != null) {
        // Update linked active rule params or create new one
        updateOrInsertActiveRuleParams(dbSession, ruleParamDto, activeRuleParamsByActiveRule);
      } else {
        // Delete linked active rule params
        deleteActiveRuleParams(dbSession, key, activeRuleParamsByActiveRule.values());
      }
      paramKeys.add(key);
    }
  }

  private void updateOrInsertActiveRuleParams(DbSession dbSession, RuleParamDto ruleParamDto, Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParamsByActiveRule) {
    activeRuleParamsByActiveRule
      .keySet()
      .forEach(new UpdateOrInsertActiveRuleParams(dbSession, dbClient, ruleParamDto, activeRuleParamsByActiveRule));
  }

  private void deleteActiveRuleParams(DbSession dbSession, String key, Collection<ActiveRuleParamDto> activeRuleParamDtos) {
    activeRuleParamDtos.forEach(new DeleteActiveRuleParams(dbSession, dbClient, key));
  }

  private static class ActiveRuleParamToActiveRule implements Function<ActiveRuleParamDto, ActiveRuleDto> {
    private final Map<Integer, OrgActiveRuleDto> activeRuleById;

    private ActiveRuleParamToActiveRule(Map<Integer, OrgActiveRuleDto> activeRuleById) {
      this.activeRuleById = activeRuleById;
    }

    @Override
    public OrgActiveRuleDto apply(@Nonnull ActiveRuleParamDto input) {
      return activeRuleById.get(input.getActiveRuleId());
    }
  }

  private static class UpdateOrInsertActiveRuleParams implements Consumer<ActiveRuleDto> {
    private final DbSession dbSession;
    private final DbClient dbClient;
    private final RuleParamDto ruleParamDto;
    private final Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParams;

    private UpdateOrInsertActiveRuleParams(DbSession dbSession, DbClient dbClient, RuleParamDto ruleParamDto, Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParams) {
      this.dbSession = dbSession;
      this.dbClient = dbClient;
      this.ruleParamDto = ruleParamDto;
      this.activeRuleParams = activeRuleParams;
    }

    @Override
    public void accept(@Nonnull ActiveRuleDto activeRuleDto) {
      Map<String, ActiveRuleParamDto> activeRuleParamByKey = from(activeRuleParams.get(activeRuleDto))
        .uniqueIndex(ActiveRuleParamDto::getKey);
      ActiveRuleParamDto activeRuleParamDto = activeRuleParamByKey.get(ruleParamDto.getName());
      if (activeRuleParamDto != null) {
        dbClient.activeRuleDao().updateParam(dbSession, activeRuleParamDto.setValue(ruleParamDto.getDefaultValue()));
      } else {
        dbClient.activeRuleDao().insertParam(dbSession, activeRuleDto, ActiveRuleParamDto.createFor(ruleParamDto).setValue(ruleParamDto.getDefaultValue()));
      }
    }
  }

  private static class DeleteActiveRuleParams implements Consumer<ActiveRuleParamDto> {
    private final DbSession dbSession;
    private final DbClient dbClient;
    private final String key;

    public DeleteActiveRuleParams(DbSession dbSession, DbClient dbClient, String key) {
      this.dbSession = dbSession;
      this.dbClient = dbClient;
      this.key = key;
    }

    @Override
    public void accept(@Nonnull ActiveRuleParamDto activeRuleParamDto) {
      if (activeRuleParamDto.getKey().equals(key)) {
        dbClient.activeRuleDao().deleteParamById(dbSession, activeRuleParamDto.getId());
      }
    }
  }

  private void update(DbSession session, RuleDto rule) {
    rule.setUpdatedAt(system.now());
    dbClient.ruleDao().update(session, rule.getDefinition());
    dbClient.ruleDao().insertOrUpdate(session, rule.getMetadata());
  }

}
