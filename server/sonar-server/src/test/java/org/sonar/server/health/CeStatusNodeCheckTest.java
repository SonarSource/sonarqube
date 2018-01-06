/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.health;

import org.junit.Test;
import org.sonar.server.app.ProcessCommandWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CeStatusNodeCheckTest {
  private ProcessCommandWrapper processCommandWrapper = mock(ProcessCommandWrapper.class);
  private CeStatusNodeCheck underTest = new CeStatusNodeCheck(processCommandWrapper);

  @Test
  public void check_returns_GREEN_status_without_cause_if_ce_is_operational() {
    when(processCommandWrapper.isCeOperational()).thenReturn(true);

    Health health = underTest.check();

    assertThat(health).isEqualTo(Health.GREEN);
  }

  @Test
  public void check_returns_RED_status_with_cause_if_ce_is_not_operational() {
    when(processCommandWrapper.isCeOperational()).thenReturn(false);

    Health health = underTest.check();

    assertThat(health.getStatus()).isEqualTo(Health.Status.RED);
    assertThat(health.getCauses()).containsOnly("Compute Engine is not operational");
  }
}
