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

import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

/**
 * List of quality profiles WS implemented in Rails.
 * New WS on quality profiles MUST be declared in {@link org.sonar.server.qualityprofile.ws.QProfilesWs}
 */
public class ProfilesWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/profiles")
      .setDescription("Former quality profiles web service");

    defineBackupAction(controller);

    controller.done();
  }

  private static void defineBackupAction(NewController controller) {
    WebService.NewAction action = controller.createAction("backup")
      .setDescription("Backup a quality profile. Requires Administer Quality Profiles permission")
      .setSince("3.1")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("language")
      .setDescription("Profile language")
      .setRequired(true)
      .setExampleValue("java");
    action.createParam("name")
      .setDescription("Profile name. If not set, the default profile for the selected language is used")
      .setExampleValue("Sonar way");
    RailsHandler.addFormatParam(action);
  }
}
