/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import java.util.Optional;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewParam;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

@ServerSide
public class QProfileWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;

  public QProfileWsSupport(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public static NewParam createOrganizationParam(NewAction action) {
    return action
            .createParam(QualityProfileWsParameters.PARAM_ORGANIZATION)
            .setDescription("Organization key.")
            .setRequired(true)
            .setExampleValue("my-org");
  }

  public OrganizationDto getOrganization(DbSession dbSession, QProfileDto profile) {
    requireNonNull(profile);
    String organizationUuid = profile.getOrganizationUuid();
    OrganizationDto organization = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
            .orElseThrow(() -> new IllegalStateException("Cannot load organization with uuid=" + organizationUuid));
    checkMembershipOnPaidOrganization(organization);
    return organization;
  }

  public OrganizationDto getOrganizationByKey(DbSession dbSession, String organizationKey) {
    OrganizationDto organization = checkFoundWithOptional(
            dbClient.organizationDao().selectByKey(dbSession, organizationKey),
            "No organization with key '%s'", organizationKey);
    checkMembershipOnPaidOrganization(organization);
    return organization;
  }

  private void checkMembershipOnPaidOrganization(OrganizationDto organization) {
    /*if (!organization.getSubscription().equals(PAID)) {
      return;
    }*/
    userSession.checkMembership(organization);
  }

  public RuleDto getRule(DbSession dbSession, RuleKey ruleKey) {
    Optional<RuleDto> ruleDefinitionDto = dbClient.ruleDao().selectByKey(dbSession, ruleKey);
    RuleDto rule = checkFoundWithOptional(ruleDefinitionDto, "Rule with key '%s' not found", ruleKey);
    checkRequest(!rule.isExternal(), "Operation forbidden for rule '%s' imported from an external rule engine.", ruleKey);
    return rule;
  }

  /**
   * Get the Quality profile specified by the reference {@code ref}.
   *
   * @throws org.sonar.server.exceptions.NotFoundException if the specified profile do not exist
   */
  public QProfileDto getProfile(DbSession dbSession, QProfileReference ref) {
    QProfileDto profile;
    if (ref.hasKey()) {
      profile = dbClient.qualityProfileDao().selectByUuid(dbSession, ref.getKey());
      checkFound(profile, "Quality Profile with key '%s' does not exist", ref.getKey());
    } else {
      OrganizationDto org = getOrganizationByKey(dbSession, ref.getOrganizationKey().orElse(null));
      profile = dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, org, ref.getName(), ref.getLanguage());
      checkFound(profile, "Quality Profile for language '%s' and name '%s' does not exist", ref.getLanguage(), ref.getName());
    }
    return profile;
  }

  public QProfileDto getProfile(DbSession dbSession, OrganizationDto organization, String name, String language) {
    QProfileDto profile = dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, organization, name, language);
    checkFound(profile, "Quality Profile for language '%s' and name '%s' does not exist in organization '%s'", language, name, organization.getKey());
    return profile;
  }

  public UserDto getUser(DbSession dbSession, OrganizationDto organization, String login) {
    UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
    checkFound(user, "User with login '%s' is not found'", login);
    checkMembership(dbSession, organization, user);
    return user;
  }

  GroupDto getGroup(DbSession dbSession, OrganizationDto organization, String groupName) {
    Optional<GroupDto> group = dbClient.groupDao().selectByName(dbSession, organization.getUuid(), groupName);
    checkFoundWithOptional(group, "No group with name '%s' in organization '%s'", groupName, organization.getKey());
    return group.get();
  }

  boolean canEdit(DbSession dbSession, OrganizationDto organization, QProfileDto profile) {
    if (canAdministrate(profile)) {
      return true;
    }
    if (userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization)) {
      return true;
    }
    UserDto user = dbClient.userDao().selectByLogin(dbSession, userSession.getLogin());
    checkState(user != null, "User from session does not exist");
    return dbClient.qProfileEditUsersDao().exists(dbSession, profile, user)
      || dbClient.qProfileEditGroupsDao().exists(dbSession, profile, userSession.getGroups());
  }

  boolean canAdministrate(QProfileDto profile) {
    if (profile.isBuiltIn() || !userSession.isLoggedIn()) {
      return false;
    }
    return userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, profile.getOrganizationUuid());
  }

  public void checkCanEdit(DbSession dbSession, OrganizationDto organization, QProfileDto profile) {
    checkNotBuiltIn(profile);
    if (!canEdit(dbSession, organization, profile)) {
      throw insufficientPrivilegesException();
    }
  }

  public void checkCanAdministrate(QProfileDto profile) {
    checkNotBuiltIn(profile);
    if (!canAdministrate(profile)) {
      throw insufficientPrivilegesException();
    }
  }

  void checkNotBuiltIn(QProfileDto profile) {
    checkRequest(!profile.isBuiltIn(), "Operation forbidden for built-in Quality Profile '%s' with language '%s'", profile.getName(), profile.getLanguage());
  }

  private void checkMembership(DbSession dbSession, OrganizationDto organization, UserDto user) {
    checkArgument(isMember(dbSession, organization, user.getUuid()),
            "User '%s' is not member of organization '%s'", user.getLogin(), organization.getKey());
  }

  private boolean isMember(DbSession dbSession, OrganizationDto organization, String userUuid) {
    return dbClient.organizationMemberDao().select(dbSession, organization.getUuid(), userUuid).isPresent();
  }

  public void checkLoggedIn() {
    userSession.checkLoggedIn();
  }
}
