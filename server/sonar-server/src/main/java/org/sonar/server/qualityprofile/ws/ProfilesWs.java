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

/**
 * List of quality profiles WS implemented in Rails.
 * New WS on quality profiles MUST be declared in {@link org.sonar.server.qualityprofile.ws.QProfilesWs}
 */
public class ProfilesWs implements WebService {

  public static final String API_ENDPOINT = "api/profiles";

  private final OldRestoreAction restoreAction;

  public ProfilesWs(OldRestoreAction restoreAction) {
    this.restoreAction = restoreAction;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(API_ENDPOINT)
      .setDescription("Old Quality Profiles. Deprecated since 5.2")
      .setSince("4.4");


    restoreAction.define(controller);
    defineListAction(controller);
    defineIndexAction(controller);

    controller.done();
  }

  private static void defineIndexAction(NewController controller) {
    WebService.NewAction action = controller.createAction("index")
      .setDescription("Get a profile.")
      .setSince("3.3")
      .setDeprecatedSince("5.2")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(ProfilesWs.class, "example-index.json"));

    action.createParam("language")
      .setDescription("Profile language")
      .setRequired(true)
      .setExampleValue("java");
    action.createParam("name")
      .setDescription("Profile name. If no profile name is given, default profile for the given language will be returned")
      .setRequired(true)
      .setExampleValue("Sonar way");
    RailsHandler.addFormatParam(action);
  }

  private static void defineListAction(NewController controller) {
    WebService.NewAction action = controller.createAction("list")
      .setDescription("Get a list of profiles.")
      .setSince("3.3")
      .setDeprecatedSince("5.2")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(ProfilesWs.class, "example-list.json"));

    action.createParam("language")
      .setDescription("Profile language")
      .setExampleValue("java");
    action.createParam("project")
      .setDescription("Project key or id")
      .setExampleValue("org.codehaus.sonar:sonar");
    RailsHandler.addJsonOnlyFormatParam(action);
  }
}
