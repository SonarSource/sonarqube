/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration;

import java.util.Date;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseMigrationStateImplTest {
  private DatabaseMigrationStateImpl underTest = new DatabaseMigrationStateImpl();

  @Test
  public void getStatus_returns_NONE_when_component_is_created() {
    assertThat(underTest.getStatus()).isEqualTo(DatabaseMigrationState.Status.NONE);
  }

  @Test
  public void getStatus_returns_argument_of_setStatus() {
    for (DatabaseMigrationState.Status status : DatabaseMigrationState.Status.values()) {
      underTest.setStatus(status);

      assertThat(underTest.getStatus()).isEqualTo(status);
    }

  }

  @Test
  public void getStartedAt_returns_null_when_component_is_created() {
    assertThat(underTest.getStartedAt()).isNull();
  }

  @Test
  public void getStartedAt_returns_argument_of_setStartedAt() {
    Date expected = new Date();
    underTest.setStartedAt(expected);

    assertThat(underTest.getStartedAt()).isSameAs(expected);
  }

  @Test
  public void getError_returns_null_when_component_is_created() {
    assertThat(underTest.getError()).isNull();
  }

  @Test
  public void getError_returns_argument_of_setError() {
    RuntimeException expected = new RuntimeException();
    underTest.setError(expected);

    assertThat(underTest.getError()).isSameAs(expected);
  }
}
