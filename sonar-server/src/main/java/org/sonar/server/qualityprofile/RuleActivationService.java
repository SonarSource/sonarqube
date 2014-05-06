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

import org.sonar.api.ServerComponent;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.server.rule2.ActiveRuleDao;
import org.sonar.server.user.UserSession;

public class RuleActivationService implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;

  public RuleActivationService(MyBatis myBatis, ActiveRuleDao activeRuleDao) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  public RuleActivation activate(RuleActivation activation, UserSession userSession) {
    verifyPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      ActiveRuleDto dto = activeRuleDao.getByKey(activation.getKey());
      if (dto == null) {
        // insert -> verify profile and parameters

      } else {
        // update -> verify parameters
      }

    } finally {
      MyBatis.closeQuietly(session);
    }

    throw new UnsupportedOperationException("TODO");
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated.
   */
  public boolean deactivate(ActiveRuleKey key, UserSession userSession) {
    verifyPermission(userSession);
    throw new UnsupportedOperationException("TODO");
  }

  /**
   * Only for rules inherited from parent profile
   */
  public RuleActivation revert(ActiveRuleKey key, UserSession userSession) {
    verifyPermission(userSession);
    throw new UnsupportedOperationException("TODO");
  }

  public void bulkActivate(BulkRuleActivation activation, UserSession userSession) {
    verifyPermission(userSession);
    throw new UnsupportedOperationException("TODO");
  }

  private void verifyPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }
}
