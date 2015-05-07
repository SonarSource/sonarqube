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
package org.sonar.server.qualityprofile;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.sonar.api.ServerSide;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.util.TypeValidations;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Activation and deactivation of rules in Quality profiles
 */
@ServerSide
public class RuleActivator {

  private final DbClient db;
  private final TypeValidations typeValidations;
  private final RuleActivatorContextFactory contextFactory;
  private final IndexClient index;
  private final ActivityService activityService;

  public RuleActivator(DbClient db, IndexClient index,
    RuleActivatorContextFactory contextFactory, TypeValidations typeValidations,
    ActivityService activityService) {
    this.db = db;
    this.index = index;
    this.contextFactory = contextFactory;
    this.typeValidations = typeValidations;
    this.activityService = activityService;
  }

  public List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation, String profileKey) {
    RuleActivatorContext context = contextFactory.create(profileKey, activation.getRuleKey(), dbSession);
    return doActivate(dbSession, activation, context);
  }

  public List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation, QProfileName profileName) {
    RuleActivatorContext context = contextFactory.create(profileName, activation.getRuleKey(), dbSession);
    return doActivate(dbSession, activation, context);
  }

  List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation, QualityProfileDto profileDto) {
    RuleActivatorContext context = contextFactory.create(profileDto, activation.getRuleKey(), dbSession);
    return doActivate(dbSession, activation, context);
  }

  private List<ActiveRuleChange> doActivate(DbSession dbSession, RuleActivation activation, RuleActivatorContext context) {
    context.verifyForActivation();
    List<ActiveRuleChange> changes = Lists.newArrayList();
    ActiveRuleChange change;
    boolean stopPropagation = false;

    ActiveRuleDto activeRule = context.activeRule();
    if (activeRule == null) {
      if (activation.isReset()) {
        // ignore reset when rule is not activated
        return changes;
      }
      // new activation
      change = ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, context.activeRuleKey());
      applySeverityAndParamToChange(activation, context, change);
      if (activation.isCascade() || context.isSameAsParent(change)) {
        change.setInheritance(ActiveRule.Inheritance.INHERITED);
      }
    } else {
      // already activated
      if (activation.isCascade() && activeRule.doesOverride()) {
        // propagating to descendants, but child profile already overrides rule -> stop propagation
        return changes;
      }
      change = ActiveRuleChange.createFor(ActiveRuleChange.Type.UPDATED, context.activeRuleKey());
      if (activation.isCascade() && activeRule.getInheritance() == null) {
        // activate on child, then on parent -> mark child as overriding parent
        change.setInheritance(ActiveRule.Inheritance.OVERRIDES);
        change.setSeverity(context.currentSeverity());
        change.setParameters(context.activeRuleParamsAsStringMap());
        stopPropagation = true;
      } else {
        applySeverityAndParamToChange(activation, context, change);
        if (!activation.isCascade() && context.parentActiveRule() != null) {
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
      changes.addAll(cascadeActivation(dbSession, activation, context.profile().getKey()));
    }

    if (!changes.isEmpty()) {
      updateProfileDate(dbSession, context);
    }
    return changes;
  }

  private void updateProfileDate(DbSession dbSession, RuleActivatorContext context) {
    context.profile().setRulesUpdatedAtAsDate(context.getInitDate());
    db.qualityProfileDao().update(dbSession, context.profile());
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
        change.setParameter(paramKey, validateParam(ruleParamDto, firstNonNull(
          context.requestParamValue(request, paramKey),
          context.currentParamValue(paramKey),
          context.parentParamValue(paramKey),
          context.defaultParamValue(paramKey))));
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
  String firstNonNull(String... strings) {
    for (String s : strings) {
      if (s != null) {
        return s;
      }
    }
    return null;
  }

  private List<ActiveRuleChange> cascadeActivation(DbSession session, RuleActivation activation, String profileKey) {
    List<ActiveRuleChange> changes = Lists.newArrayList();

    // get all inherited profiles
    List<QualityProfileDto> children = db.qualityProfileDao().findChildren(session, profileKey);
    for (QualityProfileDto child : children) {
      RuleActivation childActivation = new RuleActivation(activation).setCascade(true);
      changes.addAll(activate(session, childActivation, child.getKey()));
    }
    return changes;
  }

  private ActiveRuleDto persist(ActiveRuleChange change, RuleActivatorContext context, DbSession dbSession) {
    ActiveRuleDto activeRule = null;
    if (change.getType() == ActiveRuleChange.Type.ACTIVATED) {
      activeRule = doInsert(change, context, dbSession);

    } else if (change.getType() == ActiveRuleChange.Type.DEACTIVATED) {
      ActiveRuleDao dao = db.activeRuleDao();
      dao.deleteByKey(dbSession, change.getKey());

    } else if (change.getType() == ActiveRuleChange.Type.UPDATED) {
      activeRule = doUpdate(change, context, dbSession);
    }
    activityService.save(change.toActivity());
    return activeRule;
  }

  private ActiveRuleDto doInsert(ActiveRuleChange change, RuleActivatorContext context, DbSession dbSession) {
    ActiveRuleDto activeRule;
    ActiveRuleDao dao = db.activeRuleDao();
    activeRule = ActiveRuleDto.createFor(context.profile(), context.rule());
    String severity = change.getSeverity();
    if (severity != null) {
      activeRule.setSeverity(severity);
    }
    ActiveRule.Inheritance inheritance = change.getInheritance();
    if (inheritance != null) {
      activeRule.setInheritance(inheritance.name());
    }
    dao.insert(dbSession, activeRule);
    for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
      if (param.getValue() != null) {
        ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
        paramDto.setValue(param.getValue());
        dao.addParam(dbSession, activeRule, paramDto);
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
      dao.update(dbSession, activeRule);

      for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
        ActiveRuleParamDto activeRuleParamDto = context.activeRuleParamsAsMap().get(param.getKey());
        if (activeRuleParamDto == null) {
          // did not exist
          if (param.getValue() != null) {
            activeRuleParamDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
            activeRuleParamDto.setValue(param.getValue());
            dao.addParam(dbSession, activeRule, activeRuleParamDto);
          }
        } else {
          if (param.getValue() != null) {
            activeRuleParamDto.setValue(param.getValue());
            dao.updateParam(dbSession, activeRule, activeRuleParamDto);
          } else {
            dao.deleteParam(dbSession, activeRule, activeRuleParamDto);
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
  List<ActiveRuleChange> deactivate(ActiveRuleKey key) {
    DbSession dbSession = db.openSession(false);
    try {
      List<ActiveRuleChange> changes = deactivate(dbSession, key);
      dbSession.commit();
      return changes;
    } finally {
      dbSession.close();
    }
  }

  /**
   * Deactivate a rule on a Quality profile WITHOUT committing db session and WITHOUT checking permissions
   */
  List<ActiveRuleChange> deactivate(DbSession dbSession, ActiveRuleKey key) {
    return deactivate(dbSession, key, false);
  }

  /**
   * Deactivate a rule on a Quality profile WITHOUT committing db session, WITHOUT checking permissions, and forcing removal of inherited rules
   */
  public List<ActiveRuleChange> deactivate(DbSession dbSession, RuleDto ruleDto) {
    List<ActiveRuleChange> changes = Lists.newArrayList();
    List<ActiveRuleDto> activeRules = db.activeRuleDao().findByRule(dbSession, ruleDto);
    for (ActiveRuleDto activeRule : activeRules) {
      changes.addAll(deactivate(dbSession, activeRule.getKey(), true));
    }
    return changes;
  }

  /**
   * @param force if true then inherited rules are deactivated
   */
  public List<ActiveRuleChange> deactivate(DbSession dbSession, ActiveRuleKey key, boolean force) {
    return cascadeDeactivation(key, dbSession, false, force);
  }

  private List<ActiveRuleChange> cascadeDeactivation(ActiveRuleKey key, DbSession dbSession, boolean isCascade, boolean force) {
    List<ActiveRuleChange> changes = Lists.newArrayList();
    RuleActivatorContext context = contextFactory.create(key.qProfile(), key.ruleKey(), dbSession);
    ActiveRuleChange change;
    ActiveRuleDto activeRuleDto = context.activeRule();
    if (activeRuleDto == null) {
      return changes;
    }
    if (!force && !isCascade && activeRuleDto.getInheritance() != null) {
      throw new BadRequestException("Cannot deactivate inherited rule '" + key.ruleKey() + "'");
    }
    change = ActiveRuleChange.createFor(ActiveRuleChange.Type.DEACTIVATED, key);
    changes.add(change);
    persist(change, context, dbSession);

    // get all inherited profiles
    List<QualityProfileDto> profiles = db.qualityProfileDao().findChildren(dbSession, key.qProfile());

    for (QualityProfileDto profile : profiles) {
      ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), key.ruleKey());
      changes.addAll(cascadeDeactivation(activeRuleKey, dbSession, true, force));
    }

    if (!changes.isEmpty()) {
      updateProfileDate(dbSession, context);
    }

    return changes;
  }

  @CheckForNull
  private String validateParam(RuleParamDto ruleParam, @Nullable String value) {
    if (value != null) {
      RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
      if (ruleParamType.multiple()) {
        List<String> values = newArrayList(Splitter.on(",").split(value));
        typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
      } else {
        typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
      }
    }
    return value;
  }

  BulkChangeResult bulkActivate(RuleQuery ruleQuery, String profileKey, @Nullable String severity) {
    BulkChangeResult result = new BulkChangeResult();
    RuleIndex ruleIndex = index.get(RuleIndex.class);
    DbSession dbSession = db.openSession(false);
    try {
      Result<Rule> ruleSearchResult = ruleIndex.search(ruleQuery, new QueryContext().setScroll(true)
        .setFieldsToReturn(Arrays.asList(RuleNormalizer.RuleField.KEY.field())));
      Iterator<Rule> rules = ruleSearchResult.scroll();
      while (rules.hasNext()) {
        Rule rule = rules.next();
        try {
          RuleActivation activation = new RuleActivation(rule.key());
          activation.setSeverity(severity);
          List<ActiveRuleChange> changes = activate(dbSession, activation, profileKey);
          result.addChanges(changes);
          if (!changes.isEmpty()) {
            result.incrementSucceeded();
          }

        } catch (BadRequestException e) {
          // other exceptions stop the bulk activation
          result.incrementFailed();
          result.getErrors().add(e.errors());
        }
      }
      dbSession.commit();
    } finally {
      dbSession.close();
    }
    return result;
  }

  BulkChangeResult bulkDeactivate(RuleQuery ruleQuery, String profile) {
    DbSession dbSession = db.openSession(false);
    try {
      RuleIndex ruleIndex = index.get(RuleIndex.class);
      BulkChangeResult result = new BulkChangeResult();
      Result<Rule> ruleSearchResult = ruleIndex.search(ruleQuery, new QueryContext().setScroll(true)
        .setFieldsToReturn(Arrays.asList(RuleNormalizer.RuleField.KEY.field())));
      Iterator<Rule> rules = ruleSearchResult.scroll();
      while (rules.hasNext()) {
        try {
          Rule rule = rules.next();
          ActiveRuleKey key = ActiveRuleKey.of(profile, rule.key());
          List<ActiveRuleChange> changes = deactivate(dbSession, key);
          result.addChanges(changes);
          if (!changes.isEmpty()) {
            result.incrementSucceeded();
          }
        } catch (BadRequestException e) {
          // other exceptions stop the bulk activation
          result.incrementFailed();
          result.getErrors().add(e.errors());
        }
      }
      dbSession.commit();
      return result;
    } finally {
      dbSession.close();
    }
  }

  public void setParent(String key, @Nullable String parentKey) {
    DbSession dbSession = db.openSession(false);
    try {
      setParent(dbSession, key, parentKey);
      dbSession.commit();

    } finally {
      dbSession.close();
    }
  }

  void setParent(DbSession dbSession, String profileKey, @Nullable String parentKey) {
    QualityProfileDto profile = db.qualityProfileDao().getNonNullByKey(dbSession, profileKey);
    if (parentKey == null) {
      // unset if parent is defined, else nothing to do
      removeParent(dbSession, profile);

    } else if (profile.getParentKee() == null || !parentKey.equals(profile.getParentKee())) {
      QualityProfileDto parentProfile = db.qualityProfileDao().getNonNullByKey(dbSession, parentKey);
      if (isDescendant(dbSession, profile, parentProfile)) {
        throw new BadRequestException(String.format("Descendant profile '%s' can not be selected as parent of '%s'", parentKey, profileKey));
      }
      removeParent(dbSession, profile);

      // set new parent
      profile.setParentKee(parentKey);
      db.qualityProfileDao().update(dbSession, profile);
      for (ActiveRuleDto parentActiveRule : db.activeRuleDao().findByProfileKey(dbSession, parentKey)) {
        try {
          RuleActivation activation = new RuleActivation(parentActiveRule.getKey().ruleKey());
          activate(dbSession, activation, profileKey);
        } catch (BadRequestException e) {
          // for example because rule status is REMOVED
          // TODO return errors
        }
      }
    }
  }

  /**
   * Does not commit
   */
  private void removeParent(DbSession dbSession, QualityProfileDto profileDto) {
    if (profileDto.getParentKee() != null) {
      profileDto.setParentKee(null);
      db.qualityProfileDao().update(dbSession, profileDto);
      for (ActiveRuleDto activeRule : db.activeRuleDao().findByProfileKey(dbSession, profileDto.getKey())) {
        if (ActiveRuleDto.INHERITED.equals(activeRule.getInheritance())) {
          deactivate(dbSession, activeRule.getKey(), true);
        } else if (ActiveRuleDto.OVERRIDES.equals(activeRule.getInheritance())) {
          activeRule.setInheritance(null);
          db.activeRuleDao().update(dbSession, activeRule);
        }
      }
    }
  }

  boolean isDescendant(DbSession dbSession, QualityProfileDto childProfile, @Nullable QualityProfileDto parentProfile) {
    QualityProfileDto currentParent = parentProfile;
    while (currentParent != null) {
      if (childProfile.getName().equals(currentParent.getName())) {
        return true;
      }
      String parentKey = currentParent.getParentKee();
      if (parentKey != null) {
        currentParent = db.qualityProfileDao().getByKey(dbSession, parentKey);
      } else {
        currentParent = null;
      }
    }
    return false;
  }
}
