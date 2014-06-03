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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.index.RuleResult;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.util.TypeValidations;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Activation and deactivation of rules in Quality profiles
 */
public class RuleActivator implements ServerComponent {

  private final DbClient db;
  private final TypeValidations typeValidations;
  private final RuleActivationContextFactory contextFactory;
  private final PreviewCache previewCache;
  private final IndexClient index;

  public RuleActivator(DbClient db, IndexClient index,
                       RuleActivationContextFactory contextFactory, TypeValidations typeValidations,
                       PreviewCache previewCache) {
    this.db = db;
    this.index = index;
    this.contextFactory = contextFactory;
    this.typeValidations = typeValidations;
    this.previewCache = previewCache;
  }


  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  List<ActiveRuleChange> activate(RuleActivation activation) {
    DbSession dbSession = db.openSession(false);
    try {
      List<ActiveRuleChange> changes = activate(dbSession, activation);
      if (!changes.isEmpty()) {
        dbSession.commit();
        previewCache.reportGlobalModification();
      }
      return changes;
    } finally {
      dbSession.close();
    }
  }

  /**
   * Activate the rule WITHOUT committing db session
   */
  List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation) {

    RuleActivationContext context = contextFactory.create(activation.getKey(), dbSession);
    List<ActiveRuleChange> changes = Lists.newArrayList();
    ActiveRuleChange change;
    if (context.activeRule() == null) {
      change = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, activation.getKey());

      //Rules crated by default Inheritance
      if (activation.isCascade() || context.isSameAsParent(activation)) {
        change.setInheritance(ActiveRule.Inheritance.INHERITED);
      }
    } else {
      change = new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, activation.getKey());
      // Stop propagation on overriding profiles
      if (activation.isCascade() && context.activeRule().doesOverride()) {
        return changes;
      }

      //Updates on rule that exists with a valid parent switch them to OVERRIDE
      if (!activation.isCascade() && context.parentProfile() != null) {
        change.setInheritance(context.isSameAsParent(activation) ? ActiveRule.Inheritance.INHERITED : ActiveRule.Inheritance.OVERRIDES);
      }
    }

    // Severity and parameter values are :
    // 1. defined by end-user
    // 2. else inherited from parent profile
    // 3. else defined by rule defaults
    change.setSeverity(StringUtils.defaultIfEmpty(activation.getSeverity(), context.defaultSeverity()));
    for (RuleParamDto ruleParamDto : context.ruleParams()) {
      String value = StringUtils.defaultIfEmpty(
        activation.getParameters().get(ruleParamDto.getName()),
        context.defaultParam(ruleParamDto.getName()));
      verifyParam(ruleParamDto, value);
      change.setParameter(ruleParamDto.getName(), StringUtils.defaultIfEmpty(value, ruleParamDto.getDefaultValue()));
    }

    changes.add(change);
    // TODO filter changes without any differences

    persist(change, context, dbSession);

    // Execute the cascade on the child if NOT overrides
    changes.addAll(cascadeActivation(dbSession, activation));

    return changes;
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

  private ActiveRuleDto persist(ActiveRuleChange change, RuleActivationContext context, DbSession dbSession) {
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
        ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
        paramDto.setValue(param.getValue());
        dao.addParam(dbSession, activeRule, paramDto);
      }

    } else if (change.getType() == ActiveRuleChange.Type.DEACTIVATED) {
      dao.deleteByKey(dbSession, change.getKey());
      //activeRule = null;

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
          activeRuleParamDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
          activeRuleParamDto.setValue(param.getValue());
          dao.addParam(dbSession, activeRule, activeRuleParamDto);
        } else {
          activeRuleParamDto.setValue(param.getValue());
          dao.updateParam(dbSession, activeRule, activeRuleParamDto);
        }
      }
      for (ActiveRuleParamDto activeRuleParamDto : context.activeRuleParams()) {
        if (!change.getParameters().containsKey(activeRuleParamDto.getKey())) {
          // TODO delete param
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
    List<ActiveRuleChange> changes = Lists.newArrayList();
    try {
      changes.addAll(this.deactivate(key, dbSession));
      dbSession.commit();
    } finally {
      dbSession.close();
    }
    return changes;
  }

  /**
   * Deactivate a rule on a Quality profile WITHOUT committing db session and WITHOUT checking permissions
   */
  List<ActiveRuleChange> deactivate(ActiveRuleKey key, DbSession dbSession) {
    return cascadeDeactivation(key, dbSession, false);
  }

  private List<ActiveRuleChange> cascadeDeactivation(ActiveRuleKey key, DbSession dbSession, boolean isCascade) {
    List<ActiveRuleChange> changes = Lists.newArrayList();
    RuleActivationContext context = contextFactory.create(key, dbSession);
    ActiveRuleChange change;
    if (context.activeRule() == null) {
      return changes;
    }
    if (!isCascade && (context.activeRule().isInherited() ||
      context.activeRule().doesOverride())) {
      throw new IllegalStateException("Cannot deactivate inherited rule '" + key.ruleKey() + "'");
    }
    change = new ActiveRuleChange(ActiveRuleChange.Type.DEACTIVATED, key);
    changes.add(change);
    persist(change, context, dbSession);


    // get all inherited profiles
    List<QualityProfileDto> profiles =
      db.qualityProfileDao().findByParentKey(dbSession, key.qProfile());

    for (QualityProfileDto profile : profiles) {
      ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), key.ruleKey());
      changes.addAll(cascadeDeactivation(activeRuleKey, dbSession, true));
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

  public Multimap<String, String> bulkActivate(RuleQuery ruleQuery, QualityProfileKey profileKey, @Nullable String severity) {
    RuleIndex ruleIndex = index.get(RuleIndex.class);
    Multimap<String, String> results = ArrayListMultimap.create();
    DbSession dbSession = db.openSession(false);

    try {
      // TODO pb because limited to QueryOptions.MAX_LIMIT
      RuleResult result = ruleIndex.search(ruleQuery,
        QueryOptions.DEFAULT.setOffset(0)
          .setLimit(Integer.MAX_VALUE)
          .setFieldsToReturn(ImmutableSet.of(RuleNormalizer.RuleField.IS_TEMPLATE.field(), RuleNormalizer.RuleField.SEVERITY.field()))
      );

      for (Rule rule : result.getHits()) {
        if (!rule.isTemplate()) {
          ActiveRuleKey key = ActiveRuleKey.of(profileKey, rule.key());
          RuleActivation activation = new RuleActivation(key);
          if (severity != null && !severity.isEmpty()) {
            activation.setSeverity(severity);
          } else {
            activation.setSeverity(rule.severity());
          }
          for (ActiveRuleChange active : this.activate(dbSession, activation)) {
            results.put("activated", active.getKey().ruleKey().toString());
          }
        } else {
          results.put("ignored", rule.key().toString());
        }
      }
      dbSession.commit();
    } finally {
      dbSession.close();
    }
    return results;
  }

  public Multimap<String, String> bulkDeactivate(RuleQuery ruleQuery, QualityProfileKey profile) {
    RuleIndex ruleIndex = index.get(RuleIndex.class);
    Multimap<String, String> results = ArrayListMultimap.create();
    DbSession dbSession = db.openSession(false);

    try {
      RuleResult result = ruleIndex.search(ruleQuery,
        QueryOptions.DEFAULT.setOffset(0)
          // TODO pb because limited to QueryOptions.MAX_LIMIT
          .setLimit(Integer.MAX_VALUE)
          .setFieldsToReturn(ImmutableSet.of(RuleNormalizer.RuleField.IS_TEMPLATE.field(), RuleNormalizer.RuleField.SEVERITY.field()))
      );

      for (Rule rule : result.getHits()) {
        ActiveRuleKey key = ActiveRuleKey.of(profile, rule.key());
        for (ActiveRuleChange deActive : this.deactivate(key, dbSession)) {
          results.put("deactivated", deActive.getKey().ruleKey().toString());
        }
      }
      dbSession.commit();
    } finally {
      dbSession.close();
    }
    return results;
  }
}
