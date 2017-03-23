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

import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

@ServerSide
public class QProfileService {

  private final RuleActivator ruleActivator;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public QProfileService(RuleActivator ruleActivator, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.ruleActivator = ruleActivator;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
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
    // FIXME check for the permission of the appropriate organization, not the default one
    userSession
      .checkLoggedIn()
      .checkPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }
}
