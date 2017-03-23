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

import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class RenameAction implements QProfileWsAction {

  private static final String PARAM_PROFILE_NAME = "name";
  private static final String PARAM_PROFILE_KEY = "key";
  private static final int MAXIMUM_NAME_LENGTH = 100;

  private final DbClient dbClient;
  private final UserSession userSession;

  public RenameAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction setDefault = controller.createAction("rename")
        .setSince("5.2")
        .setDescription("Rename a quality profile. Require Administer Quality Profiles permission.")
        .setPost(true)
        .setHandler(this);

    setDefault.createParam(PARAM_PROFILE_NAME)
        .setDescription("The new name for the quality profile.")
        .setExampleValue("My Sonar way")
        .setRequired(true);

    setDefault.createParam(PARAM_PROFILE_KEY)
        .setDescription("The key of a quality profile.")
        .setExampleValue(Uuids.UUID_EXAMPLE_01)
        .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String newName = request.mandatoryParam(PARAM_PROFILE_NAME);
    String profileKey = request.mandatoryParam(PARAM_PROFILE_KEY);
    doHandle(newName, profileKey);
    response.noContent();
  }

  @VisibleForTesting
  void doHandle(String newName, String profileKey) {
    checkRequest(StringUtils.isNotBlank(newName), "Name must be set");
    checkRequest(newName.length() <= MAXIMUM_NAME_LENGTH, String.format("Name is too long (>%d characters)", MAXIMUM_NAME_LENGTH));
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityProfileDto qualityProfile = ofNullable(dbClient.qualityProfileDao().selectByKey(dbSession, profileKey))
          .orElseThrow(() -> new NotFoundException("Quality profile not found: " + profileKey));

      String organizationUuid = qualityProfile.getOrganizationUuid();
      userSession.checkPermission(ADMINISTER_QUALITY_PROFILES, organizationUuid);

      if (!Objects.equals(newName, qualityProfile.getName())) {
        OrganizationDto organization = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
            .orElseThrow(() -> new IllegalStateException("No organization found for uuid " + organizationUuid));
        String language = qualityProfile.getLanguage();
        ofNullable(dbClient.qualityProfileDao().selectByNameAndLanguage(organization, newName, language, dbSession))
            .ifPresent(found -> {
              throw BadRequestException.create(format("Quality profile already exists: %s", newName));
            });

        qualityProfile.setName(newName);
        dbClient.qualityProfileDao().update(dbSession, qualityProfile);
        dbSession.commit();
      }
    }
  }
}
