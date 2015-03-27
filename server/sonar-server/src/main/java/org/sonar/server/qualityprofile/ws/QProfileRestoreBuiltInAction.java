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
import org.sonar.server.qualityprofile.QProfileService;

public class QProfileRestoreBuiltInAction implements BaseQProfileWsAction {

  private final QProfileService service;

  public QProfileRestoreBuiltInAction(QProfileService service) {
    this.service = service;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction restoreDefault = controller.createAction("restore_built_in")
      .setDescription("Restore built-in profiles from the definitions declared by plugins. " +
        "Missing profiles are created, existing ones are updated.")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);
    restoreDefault.createParam("language")
      .setDescription("Restore the built-in profiles defined for this language")
      .setExampleValue("java")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) {
    String language = request.mandatoryParam("language");
    service.restoreBuiltInProfilesForLanguage(language);
    response.noContent();
  }

}
