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
package org.sonar.server.qualityprofile.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewParam;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;

@ServerSide
public class QProfileWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public QProfileWsSupport(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public static NewParam createOrganizationParam(NewAction action) {
    return action
      .createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org");
  }

  public OrganizationDto getOrganization(DbSession dbSession, QProfileDto profile) {
    requireNonNull(profile);
    String organizationUuid = profile.getOrganizationUuid();
    return dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
      .orElseThrow(() -> new IllegalStateException("Cannot load organization with uuid=" + organizationUuid));
  }

  public OrganizationDto getOrganizationByKey(DbSession dbSession, @Nullable String organizationKey) {
    String organizationOrDefaultKey = Optional.ofNullable(organizationKey)
      .orElseGet(defaultOrganizationProvider.get()::getKey);
    return WsUtils.checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationOrDefaultKey),
      "No organization with key '%s'", organizationOrDefaultKey);
  }

  /**
   * Get the Quality profile specified by the reference {@code ref}.
   *
   * @throws org.sonar.server.exceptions.NotFoundException if the specified organization or profile do not exist
   */
  public QProfileDto getProfile(DbSession dbSession, QProfileReference ref) {
    QProfileDto profile;
    if (ref.hasKey()) {
      profile = dbClient.qualityProfileDao().selectByUuid(dbSession, ref.getKey());
      checkFound(profile, "Quality Profile with key '%s' does not exist", ref.getKey());
    } else {
      OrganizationDto org = getOrganizationByKey(dbSession, ref.getOrganizationKey().orElse(null));
      profile = dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, org, ref.getName(), ref.getLanguage());
      checkFound(profile, "Quality Profile for language '%s' and name '%s' does not exist%s", ref.getLanguage(), ref.getName(),
        ref.getOrganizationKey().map(o -> " in organization '" + o + "'").orElse(""));
    }
    return profile;
  }

  public void checkPermission(DbSession dbSession, QProfileDto rulesProfile) {
    OrganizationDto organization = getOrganization(dbSession, rulesProfile);
    userSession.checkPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);
  }

  public void checkNotBuiltInt(QProfileDto profile) {
    checkRequest(!profile.isBuiltIn(), "Operation forbidden for built-in Quality Profile '%s' with language '%s'", profile.getName(), profile.getLanguage());
  }
}
