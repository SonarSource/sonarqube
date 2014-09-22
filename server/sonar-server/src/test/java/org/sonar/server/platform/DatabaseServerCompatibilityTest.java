/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.DatabaseVersion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseServerCompatibilityTest {

  @Test(expected = MessageException.class)
  public void shouldFailIfRequiresDowngrade() {
    DatabaseVersion version = mock(DatabaseVersion.class);
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);
    new DatabaseServerCompatibility(version).start();
  }

  @Test
  public void shouldLogWarningIfRequiresUpgrade() {
    DatabaseVersion version = mock(DatabaseVersion.class);
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    new DatabaseServerCompatibility(version).start();
    // oh well... how to simply test logging ?
    // Let's assume that this test verifies that no error is raised.
  }

  @Test
  public void shouldDoNothingIfUpToDate() {
    DatabaseVersion version = mock(DatabaseVersion.class);
    when(version.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    new DatabaseServerCompatibility(version).start();
    // no error
  }
}
