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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

@ServerSide
public class QProfileService {

  private final DbClient db;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final RuleActivator ruleActivator;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public QProfileService(DbClient db, ActiveRuleIndexer activeRuleIndexer, RuleActivator ruleActivator,
    UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.db = db;
    this.activeRuleIndexer = activeRuleIndexer;
    this.ruleActivator = ruleActivator;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  public List<ActiveRuleChange> activate(String profileKey, RuleActivation activation) {
    verifyAdminPermission();
    DbSession dbSession = db.openSession(false);
    try {
      List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, profileKey);
      dbSession.commit();
      activeRuleIndexer.index(changes);
      return changes;
    } finally {
      dbSession.close();
    }
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated, but
   * fails if the rule or the profile does not exist.
   */
  public List<ActiveRuleChange> deactivate(ActiveRuleKey key) {
    verifyAdminPermission();
    return ruleActivator.deactivate(key);
  }

  public BulkChangeResult bulkActivate(RuleQuery ruleQuery, String profile, @Nullable String severity) {
    verifyAdminPermission();
    return ruleActivator.bulkActivate(ruleQuery, profile, severity);
  }

  public BulkChangeResult bulkDeactivate(RuleQuery ruleQuery, String profile) {
    verifyAdminPermission();
    return ruleActivator.bulkDeactivate(ruleQuery, profile);
  }

  private void verifyAdminPermission() {
    userSession
      .checkLoggedIn()
      .checkPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }
}
