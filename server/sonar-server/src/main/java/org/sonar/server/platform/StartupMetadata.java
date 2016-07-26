/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static java.util.Objects.requireNonNull;

@ComputeEngineSide
@ServerSide
@Immutable
public class StartupMetadata {

  private final String startupId;
  private final long startedAt;

  public StartupMetadata(String startupId, long startedAt) {
    this.startupId = requireNonNull(startupId);
    this.startedAt = startedAt;
  }

  public String getStartupId() {
    return startupId;
  }

  public long getStartedAt() {
    return startedAt;
  }
}
