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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.log.Log;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.log.LogService;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.util.TypeValidations;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Activation and deactivation of rules in Quality profiles
 */
public class RuleActivator implements ServerComponent {

  private final DbClient db;
  private final TypeValidations typeValidations;
  private final RuleActivatorContextFactory contextFactory;
  private final PreviewCache previewCache;
  private final IndexClient index;
  private final LogService log;

  public RuleActivator(DbClient db, IndexClient index,
    RuleActivatorContextFactory contextFactory, TypeValidations typeValidations,
    PreviewCache previewCache, LogService log) {
    this.db = db;
    this.index = index;
    this.contextFactory = contextFactory;
    this.typeValidations = typeValidations;
    this.previewCache = previewCache;
    this.log = log;
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   *
   * @throws org.sonar.server.exceptions.BadRequestException if the profile, the rule or a rule parameter does
   *                                                         not exist
   */
  public List<ActiveRuleChange> activate(RuleActivation activation) {
    DbSession dbSession = db.openSession(false);
    try {
      return activate(dbSession, activation);
    } finally {
      dbSession.commit();
      dbSession.close();
    }
  }

  public List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation) {
    RuleActivatorContext context = contextFactory.create(activation.getKey(), dbSession);
    context.verifyForActivation();
    List<ActiveRuleChange> changes = Lists.newArrayList();
    ActiveRuleChange change;
    boolean stopPropagation = false;

    if (context.activeRule() == null) {
      // new activation
      change = ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, activation.getKey());
      if (activation.isCascade() || context.isSameAsParent(activation)) {
        change.setInheritance(ActiveRule.Inheritance.INHERITED);
      }
      applySeverityAndParamToChange(activation, context, change);

    } else {
      // already activated

      if (activation.isCascade() && context.activeRule().doesOverride()) {
        // propagating to descendants, but child profile already overrides rule -> stop propagation
        return changes;
      }
      change = ActiveRuleChange.createFor(ActiveRuleChange.Type.UPDATED, activation.getKey());
      if (activation.isCascade() && context.activeRule().getInheritance() == null) {
        // activate on child, then on parent -> mark child as overriding parent
        change.setInheritance(ActiveRule.Inheritance.OVERRIDES);
        change.setSeverity(context.activeRule().getSeverityString());
        change.setParameters(context.activeRuleParamsAsStringMap());
        stopPropagation = true;
      } else {
        applySeverityAndParamToChange(activation, context, change);
        if (!activation.isCascade() && context.parentProfile() != null) {
          // override rule which is already declared on parents
          change.setInheritance(context.isSameAsParent(activation) ? ActiveRule.Inheritance.INHERITED : ActiveRule.Inheritance.OVERRIDES);
        }
      }
    }

    changes.add(change);
    persist(change, context, dbSession);

    if (!stopPropagation) {
      changes.addAll(cascadeActivation(dbSession, activation));
    }

    if (!changes.isEmpty()) {
      log.write(dbSession, Log.Type.ACTIVE_RULE, changes);
      previewCache.reportGlobalModification();
    }
    return changes;
  }

  /**
   * Severity and parameter values are :
   * 1. defined by end-user
   * 2. else inherited from parent profile
   * 3. else defined by rule defaults
   *
   * On custom rules, it's always rule parameters that are used
   */
  private void applySeverityAndParamToChange(RuleActivation activation, RuleActivatorContext context, ActiveRuleChange change) {
    change.setSeverity(StringUtils.defaultIfEmpty(activation.getSeverity(), context.defaultSeverity()));
    for (RuleParamDto ruleParamDto : context.ruleParams()) {
      String value = null;
      if (context.rule().getTemplateId() == null) {
        value = StringUtils.defaultIfEmpty(
          activation.getParameters().get(ruleParamDto.getName()),
          context.defaultParam(ruleParamDto.getName()));
        verifyParam(ruleParamDto, value);
      }
      change.setParameter(ruleParamDto.getName(), StringUtils.defaultIfEmpty(value, ruleParamDto.getDefaultValue()));
    }
  }

  private List<ActiveRuleChange> cascadeActivation(DbSession session, RuleActivation activation) {
    List<ActiveRuleChange> changes = Lists.newArrayList();

    // get all inherited profiles
    List<QualityProfileDto> profiles =
      db.qualityProfileDao().findByParentKey(session, activation.getKey().qProfile());

    for (QualityProfileDto profile : profiles) {
      ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), activation.getKey().ruleKey());
      changes.addAll(this.activate(session, new RuleActivation(activeRuleKey)
        .isCascade(true)
        .setParameters(activation.getParameters())
        .setSeverity(activation.getSeverity())));
    }
    return changes;
  }

  private ActiveRuleDto persist(ActiveRuleChange change, RuleActivatorContext context, DbSession dbSession) {
    ActiveRuleDao dao = db.activeRuleDao();
    ActiveRuleDto activeRule = null;
    if (change.getType() == ActiveRuleChange.Type.ACTIVATED) {
      activeRule = ActiveRuleDto.createFor(context.profile(), context.rule());
      activeRule.setSeverity(change.getSeverity());
      if (change.getInheritance() != null) {
        activeRule.setInheritance(change.getInheritance().name());
      }
      dao.insert(dbSession, activeRule);
      for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
        if (param.getValue() != null) {
          ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
          paramDto.setValue(param.getValue());
          dao.addParam(dbSession, activeRule, paramDto);
        }
      }

    } else if (change.getType() == ActiveRuleChange.Type.DEACTIVATED) {
      dao.deleteByKey(dbSession, change.getKey());

    } else if (change.getType() == ActiveRuleChange.Type.UPDATED) {
      activeRule = context.activeRule();
      activeRule.setSeverity(change.getSeverity());
      if (change.getInheritance() != null) {
        activeRule.setInheritance(change.getInheritance().name());
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
    RuleActivatorContext context = contextFactory.create(key, dbSession);
    ActiveRuleChange change;
    if (context.activeRule() == null) {
      return changes;
    }
    if (!force && !isCascade && context.activeRule().getInheritance() != null) {
      throw new IllegalStateException("Cannot deactivate inherited rule '" + key.ruleKey() + "'");
    }
    change = ActiveRuleChange.createFor(ActiveRuleChange.Type.DEACTIVATED, key);
    changes.add(change);
    persist(change, context, dbSession);

    // get all inherited profiles
    List<QualityProfileDto> profiles = db.qualityProfileDao().findByParentKey(dbSession, key.qProfile());

    for (QualityProfileDto profile : profiles) {
      ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), key.ruleKey());
      changes.addAll(cascadeDeactivation(activeRuleKey, dbSession, true, force));
    }

    if (!changes.isEmpty()) {
      log.write(dbSession, Log.Type.ACTIVE_RULE, changes);
      previewCache.reportGlobalModification();
    }

    return changes;
  }

  private void verifyParam(RuleParamDto ruleParam, @Nullable String value) {
    if (value != null) {
      RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
      if (ruleParamType.multiple()) {
        List<String> values = newArrayList(Splitter.on(",").split(value));
        typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
      } else {
        typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
      }
    }
  }

  BulkChangeResult bulkActivate(RuleQuery ruleQuery, QualityProfileKey profileKey, @Nullable String severity) {
    BulkChangeResult result = new BulkChangeResult();
    RuleIndex ruleIndex = index.get(RuleIndex.class);
    DbSession dbSession = db.openSession(false);
    try {
      Result<Rule> ruleSearchResult = ruleIndex.search(ruleQuery, new QueryOptions().setScroll(true));
      Iterator<Rule> rules = ruleSearchResult.scroll();
      while (rules.hasNext()) {
        Rule rule = rules.next();
        try {
          ActiveRuleKey key = ActiveRuleKey.of(profileKey, rule.key());
          RuleActivation activation = new RuleActivation(key);
          activation.setSeverity(severity);
          List<ActiveRuleChange> changes = activate(dbSession, activation);
          result.addChanges(changes);
          result.incrementSucceeded();

        } catch (BadRequestException e) {
          // other exceptions stop the bulk activation
          result.incrementFailed();
          // TODO result.addMessage
        }
      }
      dbSession.commit();
    } finally {
      dbSession.close();
    }
    return result;
  }

  BulkChangeResult bulkDeactivate(RuleQuery ruleQuery, QualityProfileKey profile) {
    DbSession dbSession = db.openSession(false);
    try {
      RuleIndex ruleIndex = index.get(RuleIndex.class);
      BulkChangeResult result = new BulkChangeResult();
      Result<Rule> ruleSearchResult = ruleIndex.search(ruleQuery, new QueryOptions().setScroll(true));
      Iterator<Rule> rules = ruleSearchResult.scroll();
      while (rules.hasNext()) {
        Rule rule = rules.next();
        ActiveRuleKey key = ActiveRuleKey.of(profile, rule.key());
        List<ActiveRuleChange> changes = deactivate(dbSession, key);
        result.addChanges(changes);
        result.incrementSucceeded();
      }
      dbSession.commit();
      return result;
    } finally {
      dbSession.close();
    }
  }

  void setParent(QualityProfileKey key, @Nullable QualityProfileKey parentKey) {
    DbSession dbSession = db.openSession(false);
    try {
      setParent(dbSession, key, parentKey);
      dbSession.commit();

    } finally {
      dbSession.close();
    }
  }

  void setParent(DbSession dbSession, QualityProfileKey key, @Nullable QualityProfileKey parentKey) {
    QualityProfileDto profile = db.qualityProfileDao().getNonNullByKey(dbSession, key);
    if (parentKey == null) {
      // unset if parent is defined, else nothing to do
      removeParent(dbSession, profile);

    } else if (profile.getParentKey() == null || !profile.getParentKey().equals(parentKey)) {
      QualityProfileDto parentProfile = db.qualityProfileDao().getNonNullByKey(dbSession, parentKey);
      if (isDescendant(dbSession, profile, parentProfile)) {
        throw new BadRequestException(String.format("Descendant profile '%s' can not be selected as parent of '%s'", parentKey, key));
      }
      removeParent(dbSession, profile);

      // set new parent
      profile.setParent(parentKey.name());
      db.qualityProfileDao().update(dbSession, profile);
      for (ActiveRuleDto parentActiveRule : db.activeRuleDao().findByProfileKey(dbSession, parentKey)) {
        RuleActivation activation = new RuleActivation(ActiveRuleKey.of(key, parentActiveRule.getKey().ruleKey()));
        activate(dbSession, activation);
      }
    }
  }

  /**
   * Does not commit
   */
  private void removeParent(DbSession dbSession, QualityProfileDto profileDto) {
    if (profileDto.getParent() != null) {
      profileDto.setParent(null);
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
      if (currentParent.getParent() != null) {
        currentParent = db.qualityProfileDao().getByKey(dbSession, currentParent.getParentKey());
      } else {
        currentParent = null;
      }
    }
    return false;
  }

  private void verifyParametersAreNotSetOnCustomRule(RuleActivatorContext context, RuleActivation activation, ActiveRuleChange change) {
    if (!activation.getParameters().isEmpty() && context.rule().getTemplateId() != null) {
      throw new IllegalStateException(String.format("Parameters cannot be set when activating the custom rule '%s'", activation.getKey().ruleKey()));
    }
  }

}
