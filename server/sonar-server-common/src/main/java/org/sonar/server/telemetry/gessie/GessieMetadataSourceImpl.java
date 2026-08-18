/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.telemetry.gessie;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonarsource.gessie.server.GessieMetadataSource;
import org.sonarsource.gessie.server.InstallationDateProvider;
import org.sonarsource.gessie.server.LicenseInfoProvider;

@ServerSide
@ComputeEngineSide
public class GessieMetadataSourceImpl implements GessieMetadataSource {

  private final Server server;
  private final SonarRuntime sonarRuntime;
  private final LicenseInfoProvider licenseInfoProvider;
  private final InstallationDateProvider installationDateProvider;

  public GessieMetadataSourceImpl(Server server, SonarRuntime sonarRuntime,
    @Nullable LicenseInfoProvider licenseInfoProvider, InstallationDateProvider installationDateProvider) {
    this.server = server;
    this.sonarRuntime = sonarRuntime;
    this.licenseInfoProvider = licenseInfoProvider;
    this.installationDateProvider = installationDateProvider;
  }

  @Override
  public String getId() {
    return server.getId();
  }

  @Override
  public String getVersion() {
    return server.getVersion();
  }

  @Override
  public String getEdition() {
    return sonarRuntime.getEdition().name();
  }

  @Override
  public Optional<Long> getInstallationDate() {
    return installationDateProvider.getInstallationDate();
  }

  @Override
  public Optional<String> getLicenseType() {
    return licenseInfoProvider != null ? licenseInfoProvider.getLicenseType() : Optional.empty();
  }
}
