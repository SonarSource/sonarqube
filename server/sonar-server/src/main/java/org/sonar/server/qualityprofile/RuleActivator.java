/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
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
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Activation and deactivation of rules in Quality profiles
 */
@ServerSide
public class RuleActivator {

  private final System2 system2;
  private final DbClient db;
  private final TypeValidations typeValidations;
  private final RuleActivatorContextFactory contextFactory;
  private final RuleIndex ruleIndex;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final UserSession userSession;

  public RuleActivator(System2 system2, DbClient db, RuleIndex ruleIndex,
    RuleActivatorContextFactory contextFactory, TypeValidations typeValidations,
    ActiveRuleIndexer activeRuleIndexer, UserSession userSession) {
    this.system2 = system2;
    this.db = db;
    this.ruleIndex = ruleIndex;
    this.contextFactory = contextFactory;
    this.typeValidations = typeValidations;
    this.activeRuleIndexer = activeRuleIndexer;
    this.userSession = userSession;
  }

  public List<ActiveRuleChange> activateOnBuiltInRulesProfile(DbSession dbSession, RuleActivation activation, RulesProfileDto rulesProfile) {
    checkArgument(rulesProfile.isBuiltIn(), "Rules profile must be a built-in profile: " + rulesProfile.getKee());
    RuleActivatorContext context = contextFactory.createForBuiltIn(dbSession, activation.getRuleKey(), rulesProfile);
    return doActivate(dbSession, activation, context);
  }

  public List<ActiveRuleChange> activateAndCommit(DbSession dbSession, RuleActivation activation, QProfileDto profile) {
    List<ActiveRuleChange> changes = activate(dbSession, activation, profile);
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  public List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation, QProfileDto profile) {
    RuleActivatorContext context = contextFactory.create(dbSession, activation.getRuleKey(), profile, false);
    return doActivate(dbSession, activation, context);
  }

  private List<ActiveRuleChange> doActivate(DbSession dbSession, RuleActivation activation, RuleActivatorContext context) {
    context.verifyForActivation();
    List<ActiveRuleChange> changes = new ArrayList<>();
    ActiveRuleChange change;
    boolean stopPropagation = false;

    ActiveRuleDto activeRule = context.activeRule();
    if (activeRule == null) {
      if (activation.isReset()) {
        // ignore reset when rule is not activated
        return changes;
      }
      // new activation
      change = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, context.activeRuleKey());
      applySeverityAndParamToChange(activation, context, change);
      if (context.isCascade() || context.isSameAsParent(change)) {
        change.setInheritance(ActiveRule.Inheritance.INHERITED);
      }
    } else {
      // already activated
      if (context.isCascade() && activeRule.doesOverride()) {
        // propagating to descendants, but child profile already overrides rule -> stop propagation
        return changes;
      }
      change = new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, context.activeRuleKey());
      if (context.isCascade() && activeRule.getInheritance() == null) {
        // activate on child, then on parent -> mark child as overriding parent
        change.setInheritance(ActiveRule.Inheritance.OVERRIDES);
        change.setSeverity(context.currentSeverity());
        change.setParameters(context.activeRuleParamsAsStringMap());
        stopPropagation = true;
      } else {
        applySeverityAndParamToChange(activation, context, change);
        if (!context.isCascade() && context.parentActiveRule() != null) {
          // override rule which is already declared on parents
          change.setInheritance(context.isSameAsParent(change) ? ActiveRule.Inheritance.INHERITED : ActiveRule.Inheritance.OVERRIDES);
        }
      }
      if (context.isSame(change)) {
        change = null;
      }
    }

    if (change != null) {
      changes.add(change);
      persist(change, context, dbSession);
    }

    if (!stopPropagation) {
      changes.addAll(cascadeActivation(dbSession, activation, context));
    }

    if (!changes.isEmpty()) {
      updateProfileDates(dbSession, context);
    }
    return changes;
  }

  private void updateProfileDates(DbSession dbSession, RuleActivatorContext context) {
    QProfileDto profile = context.getProfile();
    if (profile != null) {
      profile.setRulesUpdatedAtAsDate(context.getInitDate());
      if (userSession.isLoggedIn()) {
        profile.setUserUpdatedAt(context.getInitDate().getTime());
      }
      db.qualityProfileDao().update(dbSession, profile);
    } else {
      // built-in profile, change rules_profiles.rules_updated_at
      RulesProfileDto rulesProfile = context.getRulesProfile();
      rulesProfile.setRulesUpdatedAtAsDate(context.getInitDate());
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
  private void applySeverityAndParamToChange(RuleActivation request, RuleActivatorContext context, ActiveRuleChange change) {
    if (request.isReset()) {
      // load severity and params from parent profile, else from default values
      change.setSeverity(firstNonNull(
        context.parentSeverity(), context.defaultSeverity()));
      for (RuleParamDto ruleParamDto : context.ruleParams()) {
        String paramKey = ruleParamDto.getName();
        change.setParameter(paramKey, validateParam(ruleParamDto, firstNonNull(
          context.parentParamValue(paramKey), context.defaultParamValue(paramKey))));
      }

    } else if (context.activeRule() != null) {
      // already activated -> load severity and parameters from request, else keep existing ones, else from parent,
      // else from default
      change.setSeverity(firstNonNull(
        request.getSeverity(),
        context.currentSeverity(),
        context.parentSeverity(),
        context.defaultSeverity()));
      for (RuleParamDto ruleParamDto : context.ruleParams()) {
        String paramKey = ruleParamDto.getName();
        String paramValue = context.hasRequestParamValue(request, paramKey) ?
        // If the request contains the parameter then we're using either value from request, or parent value, or default value
          firstNonNull(
            context.requestParamValue(request, paramKey),
            context.parentParamValue(paramKey),
            context.defaultParamValue(paramKey))
          // If the request doesn't contain the parameter, then we're using either value in DB, or parent value, or default value
          : firstNonNull(
            context.currentParamValue(paramKey),
            context.parentParamValue(paramKey),
            context.defaultParamValue(paramKey));
        change.setParameter(paramKey, validateParam(ruleParamDto, paramValue));
      }

    } else if (context.activeRule() == null) {
      // not activated -> load severity and parameters from request, else from parent, else from defaults
      change.setSeverity(firstNonNull(
        request.getSeverity(),
        context.parentSeverity(),
        context.defaultSeverity()));
      for (RuleParamDto ruleParamDto : context.ruleParams()) {
        String paramKey = ruleParamDto.getName();
        change.setParameter(paramKey, validateParam(ruleParamDto,
          firstNonNull(
            context.requestParamValue(request, paramKey),
            context.parentParamValue(paramKey),
            context.defaultParamValue(paramKey))));
      }
    }
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

  private List<ActiveRuleChange> cascadeActivation(DbSession dbSession, RuleActivation activation, RuleActivatorContext context) {
    List<ActiveRuleChange> changes = new ArrayList<>();

    // get all inherited profiles
    getChildren(dbSession, context).forEach(child -> {
      RuleActivatorContext childContext = contextFactory.create(dbSession, activation.getRuleKey(), child, true);
      changes.addAll(doActivate(dbSession, activation, childContext));
    });
    return changes;
  }

  protected List<QProfileDto> getChildren(DbSession session, RuleActivatorContext context) {
    if (context.getProfile() != null) {
      return db.qualityProfileDao().selectChildren(session, context.getProfile());
    }
    return db.qualityProfileDao().selectChildrenOfBuiltInRulesProfile(session, context.getRulesProfile());
  }

  private void persist(ActiveRuleChange change, RuleActivatorContext context, DbSession dbSession) {
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

  private ActiveRuleDto doInsert(ActiveRuleChange change, RuleActivatorContext context, DbSession dbSession) {
    ActiveRuleDao dao = db.activeRuleDao();
    ActiveRuleDto activeRule = new ActiveRuleDto();
    activeRule.setProfileId(context.getRulesProfile().getId());
    activeRule.setRuleId(context.getRule().getId());
    activeRule.setKey(ActiveRuleKey.of(context.getRulesProfile(), context.getRule().getKey()));
    String severity = change.getSeverity();
    if (severity != null) {
      activeRule.setSeverity(severity);
    }
    ActiveRule.Inheritance inheritance = change.getInheritance();
    if (inheritance != null) {
      activeRule.setInheritance(inheritance.name());
    }
    activeRule.setUpdatedAt(system2.now());
    activeRule.setCreatedAt(system2.now());
    dao.insert(dbSession, activeRule);
    for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
      if (param.getValue() != null) {
        ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
        paramDto.setValue(param.getValue());
        dao.insertParam(dbSession, activeRule, paramDto);
      }
    }
    return activeRule;
  }

  private ActiveRuleDto doUpdate(ActiveRuleChange change, RuleActivatorContext context, DbSession dbSession) {
    ActiveRuleDao dao = db.activeRuleDao();
    ActiveRuleDto activeRule = context.activeRule();
    if (activeRule != null) {
      String severity = change.getSeverity();
      if (severity != null) {
        activeRule.setSeverity(severity);
      }
      ActiveRule.Inheritance inheritance = change.getInheritance();
      if (inheritance != null) {
        activeRule.setInheritance(inheritance.name());
      }
      activeRule.setUpdatedAt(system2.now());
      dao.update(dbSession, activeRule);

      for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
        ActiveRuleParamDto activeRuleParamDto = context.activeRuleParamsAsMap().get(param.getKey());
        if (activeRuleParamDto == null) {
          // did not exist
          if (param.getValue() != null) {
            activeRuleParamDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
            activeRuleParamDto.setValue(param.getValue());
            dao.insertParam(dbSession, activeRule, activeRuleParamDto);
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
    }
    return activeRule;
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated, but
   * fails (fast) if the rule or the profile does not exist.
   */
  public void deactivateAndCommit(DbSession dbSession, QProfileDto profile, RuleKey ruleKey) {
    List<ActiveRuleChange> changes = deactivate(dbSession, profile, ruleKey);
    activeRuleIndexer.commitAndIndex(dbSession, changes);
  }

  /**
   * Deactivate a rule on a Quality profile WITHOUT committing db session and WITHOUT checking permissions
   */
  List<ActiveRuleChange> deactivate(DbSession dbSession, QProfileDto profile, RuleKey ruleKey) {
    return deactivate(dbSession, profile, ruleKey, false);
  }

  /**
   * Deletes a rule from all Quality profiles.
   */
  public List<ActiveRuleChange> delete(DbSession dbSession, RuleDefinitionDto rule) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    List<Integer> activeRuleIds = new ArrayList<>();

    db.activeRuleDao().selectByRuleIdOfAllOrganizations(dbSession, rule.getId()).forEach(ar -> {
      activeRuleIds.add(ar.getId());
      changes.add(new ActiveRuleChange(ActiveRuleChange.Type.DEACTIVATED, ar));
    });

    db.activeRuleDao().deleteByIds(dbSession, activeRuleIds);
    db.activeRuleDao().deleteParamsByActiveRuleIds(dbSession, activeRuleIds);

    return changes;
  }

  /**
   * @param force if true then inherited rules are deactivated
   */
  public List<ActiveRuleChange> deactivate(DbSession dbSession, QProfileDto profile, RuleKey ruleKey, boolean force) {
    RuleActivatorContext context = contextFactory.create(dbSession, ruleKey, profile, false);
    return cascadeDeactivation(dbSession, context, ruleKey, force);
  }

  public List<ActiveRuleChange> deactivateOnBuiltInRulesProfile(DbSession dbSession, RulesProfileDto rulesProfile, RuleKey ruleKey, boolean force) {
    checkArgument(rulesProfile.isBuiltIn(), "Rules profile must be a built-in profile: " + rulesProfile.getKee());
    RuleActivatorContext context = contextFactory.createForBuiltIn(dbSession, ruleKey, rulesProfile);
    return cascadeDeactivation(dbSession, context, ruleKey, force);
  }

  private List<ActiveRuleChange> cascadeDeactivation(DbSession dbSession, RuleActivatorContext context, RuleKey ruleKey, boolean force) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    ActiveRuleChange change;
    ActiveRuleDto activeRuleDto = context.activeRule();
    if (activeRuleDto == null) {
      return changes;
    }
    checkRequest(force || context.isCascade() || activeRuleDto.getInheritance() == null, "Cannot deactivate inherited rule '%s'", ruleKey);
    change = new ActiveRuleChange(ActiveRuleChange.Type.DEACTIVATED, activeRuleDto);
    changes.add(change);
    persist(change, context, dbSession);

    // get all inherited profiles (they are not built-in by design)

    getChildren(dbSession, context).forEach(child -> {
      RuleActivatorContext childContext = contextFactory.create(dbSession, ruleKey, child, true);
      changes.addAll(cascadeDeactivation(dbSession, childContext, ruleKey, force));
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

  public BulkChangeResult bulkActivateAndCommit(DbSession dbSession, RuleQuery ruleQuery, QProfileDto profile, @Nullable String severity) {
    BulkChangeResult result = new BulkChangeResult();
    Iterator<RuleKey> rules = ruleIndex.searchAll(ruleQuery);
    while (rules.hasNext()) {
      RuleKey ruleKey = rules.next();
      try {
        RuleActivation activation = RuleActivation.create(ruleKey, severity, null);
        List<ActiveRuleChange> changes = activate(dbSession, activation, profile);
        result.addChanges(changes);
        if (!changes.isEmpty()) {
          result.incrementSucceeded();
        }

      } catch (BadRequestException e) {
        // other exceptions stop the bulk activation
        result.incrementFailed();
        result.getErrors().addAll(e.errors());
      }
    }
    activeRuleIndexer.commitAndIndex(dbSession, result.getChanges());
    return result;
  }

  public BulkChangeResult bulkDeactivateAndCommit(DbSession dbSession, RuleQuery ruleQuery, QProfileDto profile) {
    BulkChangeResult result = new BulkChangeResult();
    Iterator<RuleKey> rules = ruleIndex.searchAll(ruleQuery);
    while (rules.hasNext()) {
      try {
        RuleKey ruleKey = rules.next();
        List<ActiveRuleChange> changes = deactivate(dbSession, profile, ruleKey);
        result.addChanges(changes);
        if (!changes.isEmpty()) {
          result.incrementSucceeded();
        }
      } catch (BadRequestException e) {
        // other exceptions stop the bulk activation
        result.incrementFailed();
        result.getErrors().addAll(e.errors());
      }
    }
    activeRuleIndexer.commitAndIndex(dbSession, result.getChanges());
    return result;
  }

  public List<ActiveRuleChange> setParentAndCommit(DbSession dbSession, QProfileDto profile, @Nullable QProfileDto parent) {
    checkRequest(
      parent == null || profile.getLanguage().equals(parent.getLanguage()),
      "Cannot set the profile '%s' as the parent of profile '%s' since their languages differ ('%s' != '%s')",
      parent != null ? parent.getKee() : "", profile.getKee(), parent != null ? parent.getLanguage() : "", profile.getLanguage());

    List<ActiveRuleChange> changes = new ArrayList<>();
    if (parent == null) {
      // unset if parent is defined, else nothing to do
      changes.addAll(removeParent(dbSession, profile));

    } else if (profile.getParentKee() == null || !parent.getKee().equals(profile.getParentKee())) {
      checkRequest(!isDescendant(dbSession, profile, parent), "Descendant profile '%s' can not be selected as parent of '%s'", parent.getKee(), profile.getKee());
      changes.addAll(removeParent(dbSession, profile));

      // set new parent
      profile.setParentKee(parent.getKee());
      db.qualityProfileDao().update(dbSession, profile);
      for (ActiveRuleDto parentActiveRule : db.activeRuleDao().selectByProfile(dbSession, parent)) {
        try {
          RuleActivation activation = RuleActivation.create(parentActiveRule.getRuleKey(), null, null);
          changes.addAll(activate(dbSession, activation, profile));
        } catch (BadRequestException e) {
          // for example because rule status is REMOVED
          // TODO return errors
        }
      }
    }
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  /**
   * Does not commit
   */
  private List<ActiveRuleChange> removeParent(DbSession dbSession, QProfileDto profile) {
    if (profile.getParentKee() != null) {
      List<ActiveRuleChange> changes = new ArrayList<>();
      profile.setParentKee(null);
      db.qualityProfileDao().update(dbSession, profile);
      for (ActiveRuleDto activeRule : db.activeRuleDao().selectByProfile(dbSession, profile)) {
        if (ActiveRuleDto.INHERITED.equals(activeRule.getInheritance())) {
          changes.addAll(deactivate(dbSession, profile, activeRule.getRuleKey(), true));
        } else if (ActiveRuleDto.OVERRIDES.equals(activeRule.getInheritance())) {
          activeRule.setInheritance(null);
          activeRule.setUpdatedAt(system2.now());
          db.activeRuleDao().update(dbSession, activeRule);
          changes.add(new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, activeRule).setInheritance(null));
        }
      }
      return changes;
    }
    return Collections.emptyList();
  }

  private boolean isDescendant(DbSession dbSession, QProfileDto childProfile, @Nullable QProfileDto parentProfile) {
    QProfileDto currentParent = parentProfile;
    while (currentParent != null) {
      if (childProfile.getName().equals(currentParent.getName())) {
        return true;
      }
      String parentKey = currentParent.getParentKee();
      if (parentKey != null) {
        currentParent = db.qualityProfileDao().selectByUuid(dbSession, parentKey);
      } else {
        currentParent = null;
      }
    }
    return false;
  }
}
