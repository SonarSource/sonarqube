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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.rule2.ActiveRuleDao;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ActiveRuleService implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;
  private final TypeValidations typeValidations;
  private final RuleActivationContextFactory contextFactory;

  public ActiveRuleService(MyBatis myBatis, ActiveRuleDao activeRuleDao,
                           RuleActivationContextFactory contextFactory, TypeValidations typeValidations) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
    this.contextFactory = contextFactory;
    this.typeValidations = typeValidations;
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  public List<ActiveRuleChange> activate(RuleActivation activation, UserSession userSession) {
    verifyPermission(userSession);

    DbSession dbSession = myBatis.openSession(false);
    List<ActiveRuleChange> changes = Lists.newArrayList();
    try {
      RuleActivationContext context = contextFactory.create(activation.getKey(), dbSession);
      ActiveRuleChange change;
      if (context.activeRule() == null) {
        change = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, activation.getKey());
      } else {
        change = new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, activation.getKey());
      }
      change.setSeverity(StringUtils.defaultIfEmpty(activation.getSeverity(), context.defaultSeverity()));
      // TODO params
      changes.add(change);

      // TODO apply changes to children

      persist(changes, dbSession);
      dbSession.commit();

      // TODO filter changes without any differences
      return changes;

    } finally {
      dbSession.close();
    }
  }

  private void persist(Collection<ActiveRuleChange> changes, DbSession dbSession) {
    for (ActiveRuleChange change : changes) {
      if (change.getType() == ActiveRuleChange.Type.ACTIVATED) {
        ActiveRuleDto activeRule = ActiveRuleDto.createFor(null, null /* TODO */)
          .setKey(change.getKey())
          .setSeverity(change.getSeverity());
        activeRuleDao.insert(activeRule, dbSession);

        // TODO insert activeruelparams

      } else if (change.getType() == ActiveRuleChange.Type.DEACTIVATED) {
        activeRuleDao.deleteByKey(change.getKey(), dbSession);

      } else if (change.getType() == ActiveRuleChange.Type.UPDATED) {

      }
    }
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated.
   */
  public List<ActiveRuleChange> deactivate(ActiveRuleKey key, UserSession userSession) {
    verifyPermission(userSession);
    throw new UnsupportedOperationException("TODO");
  }

  public List<ActiveRuleChange> bulkActivate(BulkRuleActivation activation, UserSession userSession) {
    verifyPermission(userSession);
    throw new UnsupportedOperationException("TODO");
  }


  private void verifyPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private void verifyParam(RuleParamDto ruleParam, String value) {
    RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
    if (ruleParamType.multiple()) {
      List<String> values = newArrayList(Splitter.on(",").split(value));
      typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
    } else {
      typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
    }
  }
}
