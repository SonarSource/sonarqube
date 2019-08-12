/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.qualityprofile.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_NAME;

public class RenameAction implements QProfileWsAction {

  private static final int MAXIMUM_NAME_LENGTH = 100;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QProfileWsSupport wsSupport;

  public RenameAction(DbClient dbClient, UserSession userSession, QProfileWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction setDefault = controller.createAction("rename")
      .setPost(true)
      .setDescription("Rename a quality profile.<br> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setSince("5.2")
      .setHandler(this);

    setDefault.createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Quality profile key")
      .setExampleValue(UUID_EXAMPLE_01);

    setDefault.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("New quality profile name")
      .setExampleValue("My Sonar way");

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String newName = request.mandatoryParam(PARAM_NAME);
    String profileKey = request.mandatoryParam(PARAM_KEY);
    doHandle(newName, profileKey);
    response.noContent();
  }

  private void doHandle(String newName, String profileKey) {
    checkRequest(newName.length() <= MAXIMUM_NAME_LENGTH, "Name is too long (>%d characters)", MAXIMUM_NAME_LENGTH);
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto qualityProfile = wsSupport.getProfile(dbSession, QProfileReference.fromKey(profileKey));
      OrganizationDto organization = wsSupport.getOrganization(dbSession, qualityProfile);
      wsSupport.checkCanEdit(dbSession, organization, qualityProfile);

      if (newName.equals(qualityProfile.getName())) {
        return;
      }

      String language = qualityProfile.getLanguage();
      ofNullable(dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, organization, newName, language))
        .ifPresent(found -> {
          throw BadRequestException.create(format("Quality profile already exists: %s", newName));
        });

      qualityProfile.setName(newName);
      dbClient.qualityProfileDao().update(dbSession, qualityProfile);
      dbSession.commit();
    }
  }
}
