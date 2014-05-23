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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.search.IndexClient;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class ActiveRuleService implements ServerComponent {

  private final DbClient db;
  private final TypeValidations typeValidations;
  private final RuleActivationContextFactory contextFactory;
  private final PreviewCache previewCache;
  private final IndexClient index;

  public ActiveRuleService(DbClient db, IndexClient index,
                           RuleActivationContextFactory contextFactory, TypeValidations typeValidations,
                           PreviewCache previewCache) {
    this.db = db;
    this.index = index;
    this.contextFactory = contextFactory;
    this.typeValidations = typeValidations;
    this.previewCache = previewCache;
  }

  @CheckForNull
  public ActiveRule getByKey(ActiveRuleKey key) {
    return index.get(ActiveRuleIndex.class).getByKey(key);
  }

  public List<ActiveRule> findByRuleKey(RuleKey key) {
    return index.get(ActiveRuleIndex.class).findByRule(key);
  }

  public List<ActiveRule> findByQProfileKey(QualityProfileKey key) {
    return index.get(ActiveRuleIndex.class).findByQProfile(key);
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  public List<ActiveRuleChange> activate(RuleActivation activation) {
    verifyPermission(UserSession.get());
    DbSession dbSession = db.openSession(false);
    try {
      List<ActiveRuleChange> changes = activate(activation, dbSession);
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
   * Activate the rule WITHOUT committing db session and WITHOUT checking permissions
   */
  List<ActiveRuleChange> activate(RuleActivation activation, DbSession dbSession) {
    List<ActiveRuleChange> changes = Lists.newArrayList();
    RuleActivationContext context = contextFactory.create(activation.getKey(), dbSession);
    ActiveRuleChange change;
    if (context.activeRule() == null) {
      change = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, activation.getKey());
    } else {
      change = new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, activation.getKey());
    }
    change.setSeverity(StringUtils.defaultIfEmpty(activation.getSeverity(), context.defaultSeverity()));
    for (RuleParamDto ruleParamDto : context.ruleParams()) {
      String value = activation.getParameters().get(ruleParamDto.getName());
      verifyParam(ruleParamDto, value);
      change.setParameter(ruleParamDto.getName(), StringUtils.defaultIfEmpty(value, ruleParamDto.getDefaultValue()));
    }
    changes.add(change);
    // TODO filter changes without any differences

    persist(changes, context, dbSession);
    return changes;
  }

  private void persist(Collection<ActiveRuleChange> changes, RuleActivationContext context, DbSession dbSession) {
    ActiveRuleDao dao = db.activeRuleDao();
    for (ActiveRuleChange change : changes) {
      if (change.getType() == ActiveRuleChange.Type.ACTIVATED) {
        ActiveRuleDto activeRule = ActiveRuleDto.createFor(context.profile(), context.rule());
        activeRule.setSeverity(change.getSeverity());
        dao.insert(activeRule, dbSession);
        for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
          ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
          paramDto.setValue(param.getValue());
          dao.addParam(activeRule, paramDto, dbSession);
        }

      } else if (change.getType() == ActiveRuleChange.Type.DEACTIVATED) {
        dao.deleteByKey(change.getKey(), dbSession);

      } else if (change.getType() == ActiveRuleChange.Type.UPDATED) {
        ActiveRuleDto activeRule = context.activeRule();
        activeRule.setSeverity(change.getSeverity());
        dao.update(activeRule, dbSession);

        for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
          ActiveRuleParamDto activeRuleParamDto = context.activeRuleParamsAsMap().get(param.getKey());
          if (activeRuleParamDto == null) {
            // did not exist
            activeRuleParamDto = ActiveRuleParamDto.createFor(context.ruleParamsByKeys().get(param.getKey()));
            activeRuleParamDto.setValue(param.getValue());
            dao.addParam(activeRule, activeRuleParamDto, dbSession);
          } else {
            activeRuleParamDto.setValue(param.getValue());
            dao.updateParam(activeRule, activeRuleParamDto, dbSession);
          }
        }
        for (ActiveRuleParamDto activeRuleParamDto : context.activeRuleParams()) {
          if (!change.getParameters().containsKey(activeRuleParamDto.getKey())) {
            // TODO delete param
          }
        }
      }
    }
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated, but
   * fails (fast) if the rule or the profile does not exist.
   */
  public List<ActiveRuleChange> deactivate(ActiveRuleKey key) {
    verifyPermission(UserSession.get());
    DbSession dbSession = db.openSession(false);
    List<ActiveRuleChange> changes = Lists.newArrayList();
    try {
      RuleActivationContext context = contextFactory.create(key, dbSession);
      ActiveRuleChange change;
      if (context.activeRule() == null) {
        // not activated !
        return changes;
      }
      change = new ActiveRuleChange(ActiveRuleChange.Type.DEACTIVATED, key);
      changes.add(change);
      persist(changes, context, dbSession);
      dbSession.commit();
      return changes;

    } finally {
      dbSession.close();
    }
  }

  public List<ActiveRuleChange> bulkActivate(BulkRuleActivation activation) {
    verifyPermission(UserSession.get());
    throw new UnsupportedOperationException("TODO");
  }


  private void verifyPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
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
}
