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
package org.sonar.server.platform;

import java.util.Optional;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.server.property.InternalProperties;
import org.sonarsource.gessie.server.InstallationDateProvider;

@ServerSide
@ComputeEngineSide
public class InstallationDateProviderImpl implements InstallationDateProvider {

  private final InternalProperties internalProperties;

  public InstallationDateProviderImpl(InternalProperties internalProperties) {
    this.internalProperties = internalProperties;
  }

  @Override
  public Optional<Long> getInstallationDate() {
    return internalProperties.read(InternalProperties.INSTALLATION_DATE)
      .map(Long::valueOf);
  }
}
