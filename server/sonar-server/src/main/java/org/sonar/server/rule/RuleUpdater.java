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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleDtoFunctions.ActiveRuleDtoToId;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.db.qualityprofile.ActiveRuleParamDtoFunctions.ActiveRuleDtoParamToKey;

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
  public boolean update(RuleUpdate update, UserSession userSession) {
    if (update.isEmpty()) {
      return false;
    }

    DbSession dbSession = dbClient.openSession(false);
    try {
      return update(dbSession, update, userSession);
    } finally {
      dbSession.close();
    }
  }

  /**
   * Update manual rules and custom rules (rules instantiated from templates)
   */
  public boolean update(DbSession dbSession, RuleUpdate update, UserSession userSession) {
    if (update.isEmpty()) {
      return false;
    }

    Context context = newContext(update);
    // validate only the changes, not all the rule fields
    apply(update, context, userSession);
    update(dbSession, context.rule);
    updateParameters(dbSession, update, context);
    dbSession.commit();
    ruleIndexer.setEnabled(true).index();
    return true;
  }

  /**
   * Load all the DTOs required for validating changes and updating rule
   */
  private Context newContext(RuleUpdate change) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Context context = new Context();
      context.rule = dbClient.ruleDao().selectOrFailByKey(dbSession, change.getRuleKey());
      if (RuleStatus.REMOVED == context.rule.getStatus()) {
        throw new IllegalArgumentException("Rule with REMOVED status cannot be updated: " + change.getRuleKey());
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
    // order is important -> sub-characteristic must be set
    if (update.isChangeDebtRemediationFunction()) {
      updateDebtRemediationFunction(update, context);
    }
  }

  private static void updateName(RuleUpdate update, Context context) {
    String name = update.getName();
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("The name is missing");
    } else {
      context.rule.setName(name);
    }
  }

  private static void updateDescription(RuleUpdate update, Context context) {
    String description = update.getMarkdownDescription();
    if (Strings.isNullOrEmpty(description)) {
      throw new IllegalArgumentException("The description is missing");
    } else {
      context.rule.setDescription(description);
      context.rule.setDescriptionFormat(RuleDto.Format.MARKDOWN);
    }
  }

  private static void updateSeverity(RuleUpdate update, Context context) {
    String severity = update.getSeverity();
    if (Strings.isNullOrEmpty(severity) || !Severity.ALL.contains(severity)) {
      throw new IllegalArgumentException("The severity is invalid");
    } else {
      context.rule.setSeverity(severity);
    }
  }

  private static void updateStatus(RuleUpdate update, Context context) {
    RuleStatus status = update.getStatus();
    if (status == null) {
      throw new IllegalArgumentException("The status is missing");
    } else {
      context.rule.setStatus(status);
    }
  }

  private static void updateTags(RuleUpdate update, Context context) {
    Set<String> tags = update.getTags();
    if (tags == null || tags.isEmpty()) {
      context.rule.setTags(Collections.<String>emptySet());
    } else {
      RuleTagHelper.applyTags(context.rule, tags);
    }
  }

  private static void updateDebtRemediationFunction(RuleUpdate update, Context context) {
    DebtRemediationFunction function = update.getDebtRemediationFunction();
    if (function == null) {
      context.rule.setRemediationFunction(null);
      context.rule.setRemediationGapMultiplier(null);
      context.rule.setRemediationBaseEffort(null);
    } else {
      if (isSameAsDefaultFunction(function, context.rule)) {
        // reset to default
        context.rule.setRemediationFunction(null);
        context.rule.setRemediationGapMultiplier(null);
        context.rule.setRemediationBaseEffort(null);
      } else {
        context.rule.setRemediationFunction(function.type().name());
        context.rule.setRemediationGapMultiplier(function.gapMultiplier());
        context.rule.setRemediationBaseEffort(function.baseEffort());
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
      context.rule.setNoteUserLogin(userSession.getLogin());
    }
  }

  private static boolean isSameAsDefaultFunction(DebtRemediationFunction fn, RuleDto rule) {
    return new EqualsBuilder()
      .append(fn.type().name(), rule.getDefaultRemediationFunction())
      .append(fn.gapMultiplier(), rule.getDefaultRemediationGapMultiplier())
      .append(fn.baseEffort(), rule.getDefaultRemediationBaseEffort())
      .isEquals();
  }

  private void updateParameters(DbSession dbSession, RuleUpdate update, Context context) {
    if (update.isChangeParameters() && update.isCustomRule()) {
      RuleDto customRule = context.rule;
      Integer templateId = customRule.getTemplateId();
      Preconditions.checkNotNull(templateId, "Rule '%s' has no persisted template!", customRule);
      Optional<RuleDto> templateRule = dbClient.ruleDao().selectById(templateId, dbSession);
      if (!templateRule.isPresent()) {
        throw new IllegalStateException(String.format("Template %s of rule %s does not exist",
          customRule.getTemplateId(), customRule.getKey()));
      }
      List<String> paramKeys = newArrayList();

      // Load active rules and its parameters in cache
      Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParamsByActiveRule = getActiveRuleParamsByActiveRule(dbSession, customRule);
      // Browse custom rule parameters to create, update or delete them
      deleteOrUpdateParameters(dbSession, update, customRule, paramKeys, activeRuleParamsByActiveRule);
    }
  }

  private Multimap<ActiveRuleDto, ActiveRuleParamDto> getActiveRuleParamsByActiveRule(DbSession dbSession, RuleDto customRule) {
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByRuleId(dbSession, customRule.getId());
    Map<Integer, ActiveRuleDto> activeRuleById = from(activeRuleDtos).uniqueIndex(ActiveRuleDtoToId.INSTANCE);
    List<Integer> activeRuleIds = from(activeRuleDtos)
      .transform(ActiveRuleDtoToId.INSTANCE)
      .toList();
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
      dbClient.ruleDao().updateRuleParam(dbSession, customRule, ruleParamDto);

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

  private void updateOrInsertActiveRuleParams(DbSession dbSession, RuleParamDto ruleParamDto, Multimap<ActiveRuleDto, ActiveRuleParamDto> activeRuleParamsByActiveRule){
    from(activeRuleParamsByActiveRule.keySet())
      .filter(new UpdateOrInsertActiveRuleParams(dbSession, dbClient, ruleParamDto, activeRuleParamsByActiveRule))
      .toList();
  }

  private void deleteActiveRuleParams(DbSession dbSession, String key, Collection<ActiveRuleParamDto> activeRuleParamDtos){
    from(activeRuleParamDtos)
      .filter(new DeleteActiveRuleParams(dbSession, dbClient, key))
      .toList();
  }

  private static class ActiveRuleParamToActiveRule implements Function<ActiveRuleParamDto, ActiveRuleDto> {
    private final Map<Integer, ActiveRuleDto> activeRuleById;

    private ActiveRuleParamToActiveRule(Map<Integer, ActiveRuleDto> activeRuleById) {
      this.activeRuleById = activeRuleById;
    }

    @Override
    public ActiveRuleDto apply(@Nonnull ActiveRuleParamDto input) {
      return activeRuleById.get(input.getActiveRuleId());
    }
  }

  private static class UpdateOrInsertActiveRuleParams implements Predicate<ActiveRuleDto> {
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
    public boolean apply(@Nonnull ActiveRuleDto activeRuleDto) {
      Map<String, ActiveRuleParamDto> activeRuleParamByKey = from(activeRuleParams.get(activeRuleDto))
        .uniqueIndex(ActiveRuleDtoParamToKey.INSTANCE);
      ActiveRuleParamDto activeRuleParamDto = activeRuleParamByKey.get(ruleParamDto.getName());
      if (activeRuleParamDto != null) {
        dbClient.activeRuleDao().updateParam(dbSession, activeRuleDto, activeRuleParamDto.setValue(ruleParamDto.getDefaultValue()));
      } else {
        dbClient.activeRuleDao().insertParam(dbSession, activeRuleDto, ActiveRuleParamDto.createFor(ruleParamDto).setValue(ruleParamDto.getDefaultValue()));
      }
      return true;
    }
  }

  private static class DeleteActiveRuleParams implements Predicate<ActiveRuleParamDto> {
    private final DbSession dbSession;
    private final DbClient dbClient;
    private final String key;

    public DeleteActiveRuleParams(DbSession dbSession, DbClient dbClient, String key) {
      this.dbSession = dbSession;
      this.dbClient = dbClient;
      this.key = key;
    }

    @Override
    public boolean apply(@Nonnull ActiveRuleParamDto activeRuleParamDto) {
      if (activeRuleParamDto.getKey().equals(key)) {
        dbClient.activeRuleDao().deleteParamById(dbSession, activeRuleParamDto.getId());
      }
      return true;
    }
  }

  /**
   * Data loaded before update
   */
  private static class Context {
    private RuleDto rule;

  }

  private void update(DbSession session, RuleDto rule) {
    rule.setUpdatedAt(system.now());
    dbClient.ruleDao().update(session, rule);
  }

}
