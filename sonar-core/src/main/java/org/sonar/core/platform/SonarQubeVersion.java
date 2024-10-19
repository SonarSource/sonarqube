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
package org.sonar.core.platform;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Version;

import static java.util.Objects.requireNonNull;

@ScannerSide
@ServerSide
@ComputeEngineSide
@Immutable
public class SonarQubeVersion {

  private final Version version;

  public SonarQubeVersion(Version version) {
    requireNonNull(version);
    this.version = version;
  }

  public Version get() {
    return this.version;
  }

  public boolean isGreaterThanOrEqual(Version than) {
    return this.version.isGreaterThanOrEqual(than);
  }

  @Override
  public String toString() {
    return version.toString();
  }
}
