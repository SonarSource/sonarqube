/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.saml.ws;

import java.util.List;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.authentication.SamlValidationRedirectionFilter;

public class SamlValidationWs implements WebService {

  public static final String SAML_VALIDATION_CONTROLLER = SamlValidationRedirectionFilter.SAML_VALIDATION_CONTROLLER_CONTEXT;
  private final List<SamlAction> actions;

  public SamlValidationWs(List<SamlAction> actions) {
    this.actions = actions;
  }

  @Override
  public void define(WebService.Context context) {
    WebService.NewController controller = context.createController(SAML_VALIDATION_CONTROLLER);
    controller.setDescription("Handle SAML validation.");
    actions.forEach(action -> action.define(controller));
    controller.done();
  }
}
