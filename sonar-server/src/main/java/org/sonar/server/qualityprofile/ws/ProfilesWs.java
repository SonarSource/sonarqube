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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class ProfilesWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/profiles")
      .setDescription("Quality profiles management");

    defineListAction(controller);
    defineBackupAction(controller);
    defineRestoreAction(controller);
    defineDestroyAction(controller);
    defineSetAsDefaultAction(controller);

    controller.done();
  }

  private void defineListAction(NewController controller) {
    WebService.NewAction action = controller.createAction("list")
      .setDescription("Get a list of profiles.")
      .setSince("3.3")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-list.json"));

    action.createParam("language")
      .setDescription("Profile language.")
      .setRequired(true)
      .setExampleValue("java");
    action.createParam("project")
      .setDescription("Project key or id.")
      .setExampleValue("org.codehaus.sonar:sonar");
  }

  private void defineBackupAction(NewController controller) {
    WebService.NewAction action = controller.createAction("backup")
      .setDescription("Backup a quality profile. Requires Administer Quality Profiles permission.")
      .setSince("3.1")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("language")
      .setDescription("Profile language.")
      .setRequired(true)
      .setExampleValue("java");
    action.createParam("name")
      .setDescription("Profile name. If not set, the default profile for the selected language is used.")
      .setExampleValue("Sonar way");
  }

  private void defineRestoreAction(NewController controller) {
    WebService.NewAction action = controller.createAction("restore")
      .setDescription("Restore a quality profile backup. Requires Administer Quality Profiles permission.")
      .setSince("3.1")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("backup")
      .setRequired(true)
      .setDescription("Path to the file containing the backup (HTML format)");
  }

  private void defineDestroyAction(NewController controller) {
    WebService.NewAction action = controller.createAction("destroy")
      .setDescription("Delete a quality profile. Requires Administer Quality Profiles permission.")
      .setSince("3.3")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("language")
      .setDescription("Profile language.")
      .setRequired(true)
      .setExampleValue("java");
    action.createParam("name")
      .setDescription("Profile name.")
      .setRequired(true)
      .setExampleValue("Sonar way");
  }

  private void defineSetAsDefaultAction(NewController controller) {
    WebService.NewAction action = controller.createAction("set_as_default")
      .setDescription("Set a quality profile as Default. Requires Administer Quality Profiles permission.")
      .setSince("3.3")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("language")
      .setDescription("Profile language.")
      .setRequired(true)
      .setExampleValue("java");
    action.createParam("name")
      .setDescription("Profile name.")
      .setRequired(true)
      .setExampleValue("Sonar way");
  }

}
