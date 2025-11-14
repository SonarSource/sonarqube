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
package org.sonar.server.platform.db.migration.step;

import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.server.platform.db.migration.version.DbVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrationStepsProviderTest {

  private final InternalMigrationStepRegistry internalMigrationStepRegistry = mock(InternalMigrationStepRegistry.class);
  private final MigrationStepsProvider underTest = new MigrationStepsProvider();

  @Test
  public void provide_throws_ISE_with_registry_build_throws_ISE_because_it_is_empty() {
    IllegalStateException expected = new IllegalStateException("faking ISE because registry is empty");
    when(internalMigrationStepRegistry.build()).thenThrow(expected);

    assertThatThrownBy(() -> underTest.provide(internalMigrationStepRegistry))
      .isInstanceOf(expected.getClass())
      .hasMessage(expected.getMessage());
  }

  @Test
  public void provide_calls_DbVersion_addStep_in_order() {
    DbVersion dbVersion1 = newMockFailingOnSecondBuildCall();
    DbVersion dbVersion2 = newMockFailingOnSecondBuildCall();
    DbVersion dbVersion3 = newMockFailingOnSecondBuildCall();
    InOrder inOrder = inOrder(dbVersion1, dbVersion2, dbVersion3);
    MigrationSteps expected = mock(MigrationSteps.class);
    when(internalMigrationStepRegistry.build()).thenReturn(expected);

    assertThat(underTest.provide(internalMigrationStepRegistry, dbVersion1, dbVersion2, dbVersion3))
      .isSameAs(expected);

    inOrder.verify(dbVersion1).addSteps(internalMigrationStepRegistry);
    inOrder.verify(dbVersion2).addSteps(internalMigrationStepRegistry);
    inOrder.verify(dbVersion3).addSteps(internalMigrationStepRegistry);
    inOrder.verifyNoMoreInteractions();
  }

  private static DbVersion newMockFailingOnSecondBuildCall() {
    DbVersion res = mock(DbVersion.class);
    doNothing()
      .doThrow(new RuntimeException("addStep should not be called twice"))
      .when(res)
      .addSteps(any(MigrationStepRegistry.class));
    return res;
  }
}
