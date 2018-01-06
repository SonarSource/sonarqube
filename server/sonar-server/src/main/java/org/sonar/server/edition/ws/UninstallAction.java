/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.edition.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.EditionManagementState.PendingStatus;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.plugins.edition.EditionInstaller;
import org.sonar.server.user.UserSession;

import static org.sonar.server.edition.EditionManagementState.PendingStatus.NONE;
import static org.sonar.server.edition.EditionManagementState.PendingStatus.UNINSTALL_IN_PROGRESS;

public class UninstallAction implements EditionsWsAction {
  private final UserSession userSession;
  private final MutableEditionManagementState mutableEditionManagementState;
  private final EditionInstaller editionInstaller;

  public UninstallAction(UserSession userSession, MutableEditionManagementState mutableEditionManagementState, EditionInstaller editionInstaller) {
    this.userSession = userSession;
    this.mutableEditionManagementState = mutableEditionManagementState;
    this.editionInstaller = editionInstaller;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("uninstall")
      .setSince("6.7")
      .setPost(true)
      .setDescription("Uninstall the currently installed edition. Requires 'Administer System' permission.")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    PendingStatus status = mutableEditionManagementState.getPendingInstallationStatus();
    if (status != NONE && status != UNINSTALL_IN_PROGRESS) {
      throw BadRequestException.create("Uninstall of the current edition is not allowed when install of an edition is in progress");
    }

    editionInstaller.uninstall();
    mutableEditionManagementState.uninstall();
    response.noContent();
  }
}
