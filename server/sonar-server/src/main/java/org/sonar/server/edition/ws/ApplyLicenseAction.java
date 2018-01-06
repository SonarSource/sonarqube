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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.edition.EditionManagementState;
import org.sonar.server.edition.License;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.license.LicenseCommit;
import org.sonar.server.platform.WebServer;
import org.sonar.server.plugins.edition.EditionInstaller;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Editions;

import static com.google.common.base.Preconditions.checkState;

public class ApplyLicenseAction implements EditionsWsAction {
  private static final String PARAM_LICENSE = "license";

  private final UserSession userSession;
  private final MutableEditionManagementState editionManagementState;
  private final EditionInstaller editionInstaller;
  private final WebServer webServer;
  @CheckForNull
  private final LicenseCommit licenseCommit;

  public ApplyLicenseAction(UserSession userSession, MutableEditionManagementState editionManagementState,
    EditionInstaller editionInstaller, WebServer webServer) {
    this(userSession, editionManagementState, editionInstaller, webServer, null);
  }

  public ApplyLicenseAction(UserSession userSession, MutableEditionManagementState editionManagementState,
    EditionInstaller editionInstaller, WebServer webServer, @Nullable LicenseCommit licenseCommit) {
    this.userSession = userSession;
    this.editionManagementState = editionManagementState;
    this.editionInstaller = editionInstaller;
    this.webServer = webServer;
    this.licenseCommit = licenseCommit;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("apply_license")
      .setSince("6.7")
      .setPost(true)
      .setDescription("Apply changes to SonarQube to match the specified license." +
        " Clear error message of previous automatic install of an edition, if there is any." +
        " Require 'Administer System' permission.")
      .setResponseExample(getClass().getResource("example-edition-apply_license.json"))
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
    License newLicense = License.parse(licenseParam).orElseThrow(() -> BadRequestException.create("The license provided is invalid"));

    if (!webServer.isStandalone()) {
      checkState(licenseCommit != null, "LicenseCommit instance not found. License-manager plugin should be installed.");
      setLicenseWithoutInstall(newLicense);
    } else if (editionInstaller.requiresInstallationChange(newLicense.getPluginKeys())) {
      editionInstaller.install(newLicense);
    } else {
      checkState(licenseCommit != null,
        "Can't decide edition does not require install if LicenseCommit instance is null. " +
          "License-manager plugin should be installed.");
      setLicenseWithoutInstall(newLicense);
    }

    WsUtils.writeProtobuf(buildResponse(), request, response);
  }

  private void setLicenseWithoutInstall(License newLicense) {
    try {
      licenseCommit.update(newLicense.getContent());
      editionManagementState.newEditionWithoutInstall(newLicense.getEditionKey());
    } catch (IllegalArgumentException e) {
      Loggers.get(ApplyLicenseAction.class).error("Failed to commit license", e);
      throw BadRequestException.create(e.getMessage());
    }
  }

  private Editions.StatusResponse buildResponse() {
    Editions.StatusResponse.Builder builder = Editions.StatusResponse.newBuilder()
      .setNextEditionKey(editionManagementState.getPendingEditionKey().orElse(""))
      .setCurrentEditionKey(editionManagementState.getCurrentEditionKey().orElse(""))
      .setInstallationStatus(Editions.InstallationStatus.valueOf(editionManagementState.getPendingInstallationStatus().name()));
    editionManagementState.getInstallErrorMessage().ifPresent(builder::setInstallError);
    return builder.build();
  }
}
