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
package org.sonar.core.scadata;

import jakarta.annotation.Priority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.sonar.api.server.ServerSide;

/**
 * Default implementation of {@link ScaDataSource} that provides default, no-op values
 * when SCA extension is not available.
 */
@ServerSide
@Priority(2)
public class DefaultScaDataSourceImpl implements ScaDataSource {
  @Override
  public List<IssueRelease> getIssueReleasesByUuids(Collection<UUID> uuids) {
    return List.of();
  }

  @Override
  public ComponentIssueAggregations getComponentIssueAggregations(String componentUuid) {
    return ComponentIssueAggregations.empty();
  }

  
  public String getCycloneDxJsonSbom(String branchUuid) {
    return "";
  }
}
