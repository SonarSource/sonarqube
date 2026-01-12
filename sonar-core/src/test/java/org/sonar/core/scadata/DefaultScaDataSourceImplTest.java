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

import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultScaDataSourceImplTest {
  private final DefaultScaDataSourceImpl dataSource = new DefaultScaDataSourceImpl();

  @Test
  public void getIssueReleasesByUuids_defaultsToEmptyList() {
    var uuids = java.util.List.of(
      UUID.fromString("00000000-000-0000-0000-000000000001"),
      UUID.fromString("00000000-000-0000-0000-000000000002"));

    var issueReleases = dataSource.getIssueReleasesByUuids(uuids);

    assertTrue(issueReleases.isEmpty());
  }

  @Test
  public void getComponentIssueAggregations_defaultsToEmpty() {
    String componentUuid = "component-uuid";

    var metrics = dataSource.getComponentIssueAggregations(componentUuid);

    assertEquals(0, metrics.issueCount());
    assertNull(metrics.finalRating());
  }
}
