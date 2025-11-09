/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.Test;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class DefaultScaDataSourceImplTest {
  private final DefaultScaDataSourceImpl dataSource = mock(DefaultScaDataSourceImpl.class);

  @Test
  public void getVulnerabilityCount_defaultsToZero() {
    String componentUuid = "component-uuid";

    int vulnerabilityCount = dataSource.getVulnerabilityCount(componentUuid);

    assertEquals(0, vulnerabilityCount);
  }

  @Test
  public void getVulnerabilityRating_defaultsToEmpty() {
    String componentUuid = "component-uuid";

    OptionalInt vulnerabilityRating = dataSource.getVulnerabilityRating(componentUuid);

    assertEquals(OptionalInt.empty(), vulnerabilityRating);
  }
}
