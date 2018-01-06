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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.EditionManagementState;
import org.sonar.server.edition.License;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.platform.WebServer;
import org.sonar.server.plugins.edition.EditionInstaller;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Editions;

import static java.util.Optional.ofNullable;
import static org.sonarqube.ws.Editions.PreviewStatus.AUTOMATIC_INSTALL;
import static org.sonarqube.ws.Editions.PreviewStatus.MANUAL_INSTALL;
import static org.sonarqube.ws.Editions.PreviewStatus.NO_INSTALL;

public class PreviewAction implements EditionsWsAction {
  private static final String PARAM_LICENSE = "license";

  private final UserSession userSession;
  private final EditionManagementState editionManagementState;
  private final EditionInstaller editionInstaller;
  private final WebServer webServer;

  public PreviewAction(UserSession userSession, EditionManagementState editionManagementState, EditionInstaller editionInstaller,
    WebServer webServer) {
    this.userSession = userSession;
    this.editionManagementState = editionManagementState;
    this.editionInstaller = editionInstaller;
    this.webServer = webServer;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("preview")
      .setSince("6.7")
      .setPost(true)
      .setDescription("Preview the changes to SonarQube to match the specified license. Requires 'Administer System' permission.")
      .setResponseExample(getClass().getResource("example-edition-preview.json"))
      .setHandler(this);

    action.createParam(PARAM_LICENSE)
      .setRequired(true)
      .setSince("6.7")
      .setDescription("the license");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();

    if (editionManagementState.getPendingInstallationStatus() != EditionManagementState.PendingStatus.NONE) {
      throw BadRequestException.create("Can't apply a license when applying one is already in progress");
    }

    String licenseParam = request.mandatoryParam(PARAM_LICENSE);
    if (licenseParam.isEmpty()) {
      throw new IllegalArgumentException(String.format("The '%s' parameter is empty", PARAM_LICENSE));
    }
    License newLicense = License.parse(licenseParam).orElseThrow(() -> BadRequestException.create("The license provided is invalid"));

    NextState nextState = computeNextState(newLicense);
    WsUtils.writeProtobuf(buildResponse(nextState), request, response);
  }

  private static Editions.PreviewResponse buildResponse(NextState nextState) {
    return Editions.PreviewResponse.newBuilder()
      .setNextEditionKey(nextState.getPendingEditionKey().orElse(""))
      .setPreviewStatus(nextState.getPreviewStatus())
      .build();
  }

  private NextState computeNextState(License newLicense) {
    if (!webServer.isStandalone() || !editionInstaller.requiresInstallationChange(newLicense.getPluginKeys())) {
      return new NextState(newLicense.getEditionKey(), NO_INSTALL);
      // this won't refresh the update center (uses cached state). Preview is called while typing (must be fast)
      // and anyway the status is refreshed when arriving at the marketplace page.
    } else if (editionInstaller.isOffline()) {
      return new NextState(newLicense.getEditionKey(), MANUAL_INSTALL);
    } else {
      return new NextState(newLicense.getEditionKey(), AUTOMATIC_INSTALL);
    }
  }

  private static final class NextState {
    private final String pendingEditionKey;
    private final Editions.PreviewStatus previewStatus;

    private NextState(@Nullable String pendingEditionKey, Editions.PreviewStatus previewStatus) {
      this.pendingEditionKey = pendingEditionKey;
      this.previewStatus = previewStatus;
    }

    Optional<String> getPendingEditionKey() {
      return ofNullable(pendingEditionKey);
    }

    Editions.PreviewStatus getPreviewStatus() {
      return previewStatus;
    }
  }

}
