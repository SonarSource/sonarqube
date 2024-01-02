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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkState;
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
      profile = dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, ref.getName(), ref.getLanguage());
      checkFound(profile, "Quality Profile for language '%s' and name '%s' does not exist", ref.getLanguage(), ref.getName());
    }
    return profile;
  }

  public QProfileDto getProfile(DbSession dbSession, String name, String language) {
    QProfileDto profile = dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, name, language);
    checkFound(profile, "Quality Profile for language '%s' and name '%s' does not exist", language, name);
    return profile;
  }

  public UserDto getUser(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
    checkFound(user, "User with login '%s' is not found'", login);
    return user;
  }

  GroupDto getGroup(DbSession dbSession, String groupName) {
    Optional<GroupDto> group = dbClient.groupDao().selectByName(dbSession, groupName);
    checkFoundWithOptional(group, "No group with name '%s'", groupName);
    return group.get();
  }

  boolean canEdit(DbSession dbSession, QProfileDto profile) {
    if (canAdministrate(profile)) {
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
    return userSession.hasPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);
  }

  public void checkCanEdit(DbSession dbSession, QProfileDto profile) {
    checkNotBuiltIn(profile);
    if (!canEdit(dbSession, profile)) {
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
}
