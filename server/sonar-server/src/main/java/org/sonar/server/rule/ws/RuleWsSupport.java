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
package org.sonar.server.rule.ws;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

@ServerSide
public class RuleWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RuleWsSupport(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public void checkQProfileAdminPermissionOnDefaultOrganization() {
    userSession
      .checkLoggedIn()
      .checkPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }

  public OrganizationDto getOrganizationByKey(DbSession dbSession, @Nullable String organizationKey) {
    String organizationOrDefaultKey = Optional.ofNullable(organizationKey)
      .orElseGet(defaultOrganizationProvider.get()::getKey);
    OrganizationDto organization = checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationOrDefaultKey),
      "No organization with key '%s'", organizationOrDefaultKey);
    checkMembershipOnPaidOrganization(organization);
    return organization;
  }

  Map<String, UserDto> getUsersByUuid(DbSession dbSession, List<RuleDto> rules) {
    Set<String> userUuids = rules.stream().map(RuleDto::getNoteUserUuid).filter(Objects::nonNull).collect(toSet());
    return dbClient.userDao().selectByUuids(dbSession, userUuids).stream().collect(uniqueIndex(UserDto::getUuid));
  }

  void checkMembershipOnPaidOrganization(OrganizationDto organization) {
    if (!organization.getSubscription().equals(PAID)) {
      return;
    }
    userSession.checkMembership(organization);
  }

}
