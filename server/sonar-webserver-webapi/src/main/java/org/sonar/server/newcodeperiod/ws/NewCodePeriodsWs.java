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
package org.sonar.server.newcodeperiod.ws;

import org.sonar.api.server.ws.WebService;
import org.sonar.core.documentation.DocumentationLinkGenerator;

import static org.sonar.server.ws.WsUtils.createHtmlExternalLink;

public class NewCodePeriodsWs implements WebService {

  private final NewCodePeriodsWsAction[] actions;
  private final DocumentationLinkGenerator documentationLinkGenerator;

  public NewCodePeriodsWs(DocumentationLinkGenerator documentationLinkGenerator, NewCodePeriodsWsAction... actions) {
    this.actions = actions;
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/new_code_periods")
      .setDescription("Manage "+ createHtmlExternalLink(documentationLinkGenerator.getDocumentationLink("/project-administration/clean-as-you-code-settings/defining-new-code/"), "new code definition") +".")
      .setSince("8.0");
    for (NewCodePeriodsWsAction action : actions) {
      action.define(controller);
    }
    controller.done();
  }
}
