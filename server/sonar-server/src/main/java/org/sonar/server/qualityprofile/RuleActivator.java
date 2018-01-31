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
package org.sonar.server.qualityprofile;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.RuleActivationContext.ActiveRuleWrapper;
import org.sonar.server.qualityprofile.RuleActivationContext.RuleWrapper;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singleton;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Activation and deactivation of rules in Quality profiles
 */
@ServerSide
public class RuleActivator {

  private final System2 system2;
  private final DbClient db;
  private final TypeValidations typeValidations;
  private final UserSession userSession;

  public RuleActivator(System2 system2, DbClient db, TypeValidations typeValidations, UserSession userSession) {
    this.system2 = system2;
    this.db = db;
    this.typeValidations = typeValidations;
    this.userSession = userSession;
  }

  public List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation, RuleActivationContext context) {
    context.reset(activation.getRuleId());
    return doActivate(dbSession, activation, context);
  }

  private List<ActiveRuleChange> doActivate(DbSession dbSession, RuleActivation activation, RuleActivationContext context) {
    RuleDefinitionDto rule = context.getRule().get();
    checkRequest(RuleStatus.REMOVED != rule.getStatus(), "Rule was removed: %s", rule.getKey());
    checkRequest(!rule.isTemplate(), "Rule template can't be activated on a Quality profile: %s", rule.getKey());

    List<ActiveRuleChange> changes = new ArrayList<>();
    ActiveRuleChange change;
    boolean stopCascading = false;

    ActiveRuleWrapper activeRule = context.getActiveRule();
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(context.getRulesProfile(), rule.getKey());
    if (activeRule == null) {
      if (activation.isReset()) {
        // ignore reset when rule is not activated
        return changes;
      }
      // new activation
      change = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, activeRuleKey, rule);
      applySeverityAndParamToChange(activation, context, change);
      if (context.isCascading() || isSameAsParent(change, context)) {
        change.setInheritance(ActiveRuleInheritance.INHERITED);
      }
    } else {
      // already activated
      if (context.isCascading() && activeRule.get().doesOverride()) {
        // propagating to descendants, but child profile already overrides rule -> stop propagation
        return changes;
      }
      change = new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, activeRuleKey, rule);
      if (context.isCascading() && activeRule.get().getInheritance() == null) {
        // activate on child, then on parent -> mark child as overriding parent
        change.setInheritance(ActiveRuleInheritance.OVERRIDES);
        change.setSeverity(activeRule.get().getSeverityString());
        for (ActiveRuleParamDto activeParam : activeRule.getParams()) {
          change.setParameter(activeParam.getKey(), activeParam.getValue());
        }
        stopCascading = true;
      } else {
        applySeverityAndParamToChange(activation, context, change);
        if (!context.isCascading() && context.getParentActiveRule() != null) {
          // override rule which is already declared on parents
          change.setInheritance(isSameAsParent(change, context) ? ActiveRuleInheritance.INHERITED : ActiveRuleInheritance.OVERRIDES);
        }
      }
      if (isSame(change, activeRule)) {
        change = null;
      }
    }

    if (change != null) {
      changes.add(change);
      persist(change, context, dbSession);
    }

    if (!stopCascading) {
      changes.addAll(propagateActivationToDescendants(dbSession, activation, context));
    }

    if (!changes.isEmpty()) {
      updateProfileDates(dbSession, context);
    }
    return changes;
  }

  private void updateProfileDates(DbSession dbSession, RuleActivationContext context) {
    QProfileDto profile = context.getProfile();
    if (profile != null) {
      profile.setRulesUpdatedAtAsDate(new Date(context.getDate()));
      if (userSession.isLoggedIn()) {
        profile.setUserUpdatedAt(context.getDate());
      }
      db.qualityProfileDao().update(dbSession, profile);

    } else {
      // built-in profile, change rules_profiles.rules_updated_at
      RulesProfileDto rulesProfile = context.getRulesProfile();
      rulesProfile.setRulesUpdatedAtAsDate(new Date(context.getDate()));
      db.qualityProfileDao().update(dbSession, rulesProfile);
    }
  }

  /**
   * Severity and parameter values are :
   * 1. defined by end-user
   * 2. else inherited from parent profile
   * 3. else defined by rule defaults
   * <p/>
   * On custom rules, it's always rule parameters that are used
   */
  private void applySeverityAndParamToChange(RuleActivation request, RuleActivationContext context, ActiveRuleChange change) {
    RuleWrapper rule = context.getRule();
    ActiveRuleWrapper activeRule = context.getActiveRule();
    ActiveRuleWrapper parentActiveRule = context.getParentActiveRule();

    if (request.isReset()) {
      // load severity and params from parent profile, else from default values
      change.setSeverity(firstNonNull(
        parentActiveRule != null ? parentActiveRule.get().getSeverityString() : null,
        rule.get().getSeverityString()));

      for (RuleParamDto ruleParamDto : rule.getParams()) {
        String paramKey = ruleParamDto.getName();
        change.setParameter(paramKey, validateParam(ruleParamDto, firstNonNull(
          parentActiveRule != null ? parentActiveRule.getParamValue(paramKey) : null,
          rule.getParamDefaultValue(paramKey))));
      }

    } else if (activeRule != null) {
      // already activated -> load severity and parameters from request, else keep existing ones, else from parent,
      // else from default
      change.setSeverity(firstNonNull(
        request.getSeverity(),
        activeRule.get().getSeverityString(),
        parentActiveRule != null ? parentActiveRule.get().getSeverityString() : null,
        rule.get().getSeverityString()));
      for (RuleParamDto ruleParamDto : rule.getParams()) {
        String paramKey = ruleParamDto.getName();
        String parentValue = parentActiveRule != null ? parentActiveRule.getParamValue(paramKey) : null;
        String paramValue = context.hasRequestedParamValue(request, paramKey) ?
        // If the request contains the parameter then we're using either value from request, or parent value, or default value
          firstNonNull(
            context.getRequestedParamValue(request, paramKey),
            parentValue,
            rule.getParamDefaultValue(paramKey))
          // If the request doesn't contain the parameter, then we're using either value in DB, or parent value, or default value
          : firstNonNull(
            activeRule.getParamValue(paramKey),
            parentValue,
            rule.getParamDefaultValue(paramKey));
        change.setParameter(paramKey, validateParam(ruleParamDto, paramValue));
      }

    } else {
      // not activated -> load severity and parameters from request, else from parent, else from defaults
      change.setSeverity(firstNonNull(
        request.getSeverity(),
        parentActiveRule != null ? parentActiveRule.get().getSeverityString() : null,
        rule.get().getSeverityString()));
      for (RuleParamDto ruleParamDto : rule.getParams()) {
        String paramKey = ruleParamDto.getName();
        change.setParameter(paramKey, validateParam(ruleParamDto,
          firstNonNull(
            context.getRequestedParamValue(request, paramKey),
            parentActiveRule != null ? parentActiveRule.getParamValue(paramKey) : null,
            rule.getParamDefaultValue(paramKey))));
      }
    }
  }

  private List<ActiveRuleChange> propagateActivationToDescendants(DbSession dbSession, RuleActivation activation, RuleActivationContext context) {
    List<ActiveRuleChange> changes = new ArrayList<>();

    // get all inherited profiles
    context.getChildProfiles().forEach(child -> {
      context.switchToChild(child);
      changes.addAll(doActivate(dbSession, activation, context));
    });
    return changes;
  }

  private void persist(ActiveRuleChange change, RuleActivationContext context, DbSession dbSession) {
    ActiveRuleDto activeRule = null;
    if (change.getType() == ActiveRuleChange.Type.ACTIVATED) {
      activeRule = doInsert(change, context, dbSession);
    } else if (change.getType() == ActiveRuleChange.Type.DEACTIVATED) {
      ActiveRuleDao dao = db.activeRuleDao();
      activeRule = dao.delete(dbSession, change.getKey()).orElse(null);

    } else if (change.getType() == ActiveRuleChange.Type.UPDATED) {
      activeRule = doUpdate(change, context, dbSession);
    }
    change.setActiveRule(activeRule);
    db.qProfileChangeDao().insert(dbSession, change.toDto(userSession.getLogin()));
  }

  private ActiveRuleDto doInsert(ActiveRuleChange change, RuleActivationContext context, DbSession dbSession) {
    ActiveRuleDao dao = db.activeRuleDao();
    RuleWrapper rule = context.getRule();

    ActiveRuleDto activeRule = new ActiveRuleDto();
    activeRule.setProfileId(context.getRulesProfile().getId());
    activeRule.setRuleId(rule.get().getId());
    activeRule.setKey(ActiveRuleKey.of(context.getRulesProfile(), rule.get().getKey()));
    String severity = change.getSeverity();
    if (severity != null) {
      activeRule.setSeverity(severity);
    }
    ActiveRuleInheritance inheritance = change.getInheritance();
    if (inheritance != null) {
      activeRule.setInheritance(inheritance.name());
    }
    activeRule.setUpdatedAt(system2.now());
    activeRule.setCreatedAt(system2.now());
    dao.insert(dbSession, activeRule);
    for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
      if (param.getValue() != null) {
        ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(rule.getParam(param.getKey()));
        paramDto.setValue(param.getValue());
        dao.insertParam(dbSession, activeRule, paramDto);
      }
    }
    return activeRule;
  }

  private ActiveRuleDto doUpdate(ActiveRuleChange change, RuleActivationContext context, DbSession dbSession) {
    ActiveRuleWrapper activeRule = context.getActiveRule();
    if (activeRule == null) {
      return null;
    }
    ActiveRuleDao dao = db.activeRuleDao();
    String severity = change.getSeverity();
    if (severity != null) {
      activeRule.get().setSeverity(severity);
    }
    ActiveRuleInheritance inheritance = change.getInheritance();
    if (inheritance != null) {
      activeRule.get().setInheritance(inheritance.name());
    }
    activeRule.get().setUpdatedAt(system2.now());
    dao.update(dbSession, activeRule.get());

    for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
      ActiveRuleParamDto activeRuleParamDto = activeRule.getParam(param.getKey());
      if (activeRuleParamDto == null) {
        // did not exist
        if (param.getValue() != null) {
          activeRuleParamDto = ActiveRuleParamDto.createFor(context.getRule().getParam(param.getKey()));
          activeRuleParamDto.setValue(param.getValue());
          dao.insertParam(dbSession, activeRule.get(), activeRuleParamDto);
        }
      } else {
        if (param.getValue() != null) {
          activeRuleParamDto.setValue(param.getValue());
          dao.updateParam(dbSession, activeRuleParamDto);
        } else {
          dao.deleteParam(dbSession, activeRuleParamDto);
        }
      }
    }
    return activeRule.get();
  }

  public List<ActiveRuleChange> deactivate(DbSession dbSession, RuleActivationContext context, int ruleId, boolean force) {
    context.reset(ruleId);
    return doDeactivate(dbSession, context, force);
  }

  private List<ActiveRuleChange> doDeactivate(DbSession dbSession, RuleActivationContext context, boolean force) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    ActiveRuleWrapper activeRule = context.getActiveRule();
    if (activeRule == null) {
      return changes;
    }

    ActiveRuleChange change;
    checkRequest(force || context.isCascading() || activeRule.get().getInheritance() == null, "Cannot deactivate inherited rule '%s'", context.getRule().get().getKey());
    change = new ActiveRuleChange(ActiveRuleChange.Type.DEACTIVATED, activeRule.get(), context.getRule().get());
    changes.add(change);
    persist(change, context, dbSession);

    // get all inherited profiles (they are not built-in by design)
    context.getChildProfiles().forEach(child -> {
      context.switchToChild(child);
      changes.addAll(doDeactivate(dbSession, context, force));
    });

    if (!changes.isEmpty()) {
      updateProfileDates(dbSession, context);
    }

    return changes;
  }

  @CheckForNull
  private String validateParam(RuleParamDto ruleParam, @Nullable String value) {
    if (value != null) {
      RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
      if (ruleParamType.multiple()) {
        List<String> values = Splitter.on(",").splitToList(value);
        typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
      } else {
        typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
      }
    }
    return value;
  }

  public RuleActivationContext createContextForBuiltInProfile(DbSession dbSession, RulesProfileDto builtInProfile, Collection<Integer> ruleIds) {
    checkArgument(builtInProfile.isBuiltIn(), "Rules profile with UUID %s is not built-in", builtInProfile.getKee());

    RuleActivationContext.Builder builder = new RuleActivationContext.Builder();

    // load rules
    List<RuleDefinitionDto> rules = completeWithRules(dbSession, builder, ruleIds);

    // load profiles
    List<QProfileDto> aliasedBuiltInProfiles = db.qualityProfileDao().selectQProfilesByRuleProfile(dbSession, builtInProfile);
    List<QProfileDto> profiles = new ArrayList<>(aliasedBuiltInProfiles);
    profiles.addAll(db.qualityProfileDao().selectDescendants(dbSession, aliasedBuiltInProfiles));
    builder.setProfiles(profiles);
    builder.setBaseProfile(builtInProfile);

    // load active rules
    Collection<String> ruleProfileUuids = Stream
      .concat(Stream.of(builtInProfile.getKee()), profiles.stream().map(QProfileDto::getRulesProfileUuid))
      .collect(MoreCollectors.toHashSet(profiles.size() + 1));
    completeWithActiveRules(dbSession, builder, rules, ruleProfileUuids);

    return builder.build();
  }

  public RuleActivationContext createContextForUserProfile(DbSession dbSession, QProfileDto profile, Collection<Integer> ruleIds) {
    checkArgument(!profile.isBuiltIn(), "Profile with UUID %s is built-in", profile.getKee());
    RuleActivationContext.Builder builder = new RuleActivationContext.Builder();

    // load rules
    List<RuleDefinitionDto> rules = completeWithRules(dbSession, builder, ruleIds);

    // load descendant profiles
    List<QProfileDto> profiles = new ArrayList<>(db.qualityProfileDao().selectDescendants(dbSession, singleton(profile)));
    profiles.add(profile);
    if (profile.getParentKee() != null) {
      profiles.add(db.qualityProfileDao().selectByUuid(dbSession, profile.getParentKee()));
    }
    builder.setProfiles(profiles);
    builder.setBaseProfile(profile);

    // load active rules
    Collection<String> ruleProfileUuids = profiles.stream()
      .map(QProfileDto::getRulesProfileUuid)
      .collect(MoreCollectors.toHashSet(profiles.size()));
    completeWithActiveRules(dbSession, builder, rules, ruleProfileUuids);

    return builder.build();
  }

  private List<RuleDefinitionDto> completeWithRules(DbSession dbSession, RuleActivationContext.Builder builder, Collection<Integer> ruleIds) {
    List<RuleDefinitionDto> rules = db.ruleDao().selectDefinitionByIds(dbSession, ruleIds);
    builder.setRules(rules);
    builder.setRuleParams(db.ruleDao().selectRuleParamsByRuleIds(dbSession, ruleIds));
    return rules;
  }

  private void completeWithActiveRules(DbSession dbSession, RuleActivationContext.Builder builder, Collection<RuleDefinitionDto> rules, Collection<String> ruleProfileUuids) {
    Collection<ActiveRuleDto> activeRules = db.activeRuleDao().selectByRulesAndRuleProfileUuids(dbSession, rules, ruleProfileUuids);
    builder.setActiveRules(activeRules);
    List<Integer> activeRuleIds = activeRules.stream().map(ActiveRuleDto::getId).collect(MoreCollectors.toArrayList(activeRules.size()));
    builder.setActiveRuleParams(db.activeRuleDao().selectParamsByActiveRuleIds(dbSession, activeRuleIds));
  }

  private static boolean isSame(ActiveRuleChange change, ActiveRuleWrapper activeRule) {
    ActiveRuleInheritance inheritance = change.getInheritance();
    if (inheritance != null && !inheritance.name().equals(activeRule.get().getInheritance())) {
      return false;
    }
    String severity = change.getSeverity();
    if (severity != null && !severity.equals(activeRule.get().getSeverityString())) {
      return false;
    }
    for (Map.Entry<String, String> changeParam : change.getParameters().entrySet()) {
      String activeParamValue = activeRule.getParamValue(changeParam.getKey());
      if (changeParam.getValue() == null && activeParamValue != null) {
        return false;
      }
      if (changeParam.getValue() != null && (activeParamValue == null || !StringUtils.equals(changeParam.getValue(), activeParamValue))) {
        return false;
      }
    }
    return true;
  }

  /**
   * True if trying to override an inherited rule but with exactly the same values
   */
  private static boolean isSameAsParent(ActiveRuleChange change, RuleActivationContext context) {
    ActiveRuleWrapper parentActiveRule = context.getParentActiveRule();
    if (parentActiveRule == null) {
      return false;
    }
    if (!StringUtils.equals(change.getSeverity(), parentActiveRule.get().getSeverityString())) {
      return false;
    }
    for (Map.Entry<String, String> entry : change.getParameters().entrySet()) {
      if (entry.getValue() != null && !entry.getValue().equals(parentActiveRule.getParamValue(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  @CheckForNull
  private static String firstNonNull(String... strings) {
    for (String s : strings) {
      if (s != null) {
        return s;
      }
    }
    return null;
  }
}
