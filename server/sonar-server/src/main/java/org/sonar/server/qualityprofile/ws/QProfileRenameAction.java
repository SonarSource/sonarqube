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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.UserSession;

public class QProfileRenameAction implements BaseQProfileWsAction {

  private static final String PARAM_PROFILE_NAME = "name";
  private static final String PARAM_PROFILE_KEY = "key";

  private final QProfileFactory profileFactory;

  public QProfileRenameAction(QProfileFactory profileFactory) {
    this.profileFactory = profileFactory;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction setDefault = controller.createAction("rename")
      .setSince("5.2")
      .setDescription("Rename a quality profile.")
      .setPost(true)
      .setHandler(this);

    setDefault.createParam(PARAM_PROFILE_NAME)
      .setDescription("The new name for the quality profile.")
      .setExampleValue("My Sonar way")
      .setRequired(true);

    setDefault.createParam(PARAM_PROFILE_KEY)
      .setDescription("The key of a quality profile.")
      .setExampleValue("sonar-way-js-12345")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkLoggedIn().checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    String newName = request.mandatoryParam(PARAM_PROFILE_NAME);
    String profileKey = request.mandatoryParam(PARAM_PROFILE_KEY);

    profileFactory.rename(profileKey, newName);

    response.noContent();
  }
}
