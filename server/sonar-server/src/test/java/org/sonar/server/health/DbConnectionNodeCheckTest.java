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
package org.sonar.server.health;

import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.IsAliveMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbConnectionNodeCheckTest {
  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private IsAliveMapper isAliveMapper = mock(IsAliveMapper.class);

  private DbConnectionNodeCheck underTest = new DbConnectionNodeCheck(dbClient);

  @Before
  public void wireMocks() {
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(dbSession.getMapper(IsAliveMapper.class)).thenReturn(isAliveMapper);
  }

  @Test
  public void status_is_GREEN_without_cause_if_isAlive_returns_1() {
    when(isAliveMapper.isAlive()).thenReturn(1);

    Health health = underTest.check();

    assertThat(health).isEqualTo(Health.GREEN);
  }

  @Test
  public void status_is_RED_with_single_cause_if_any_error_occurs_when_checking_DB() {
    when(isAliveMapper.isAlive()).thenThrow(new RuntimeException("simulated runtime exception when querying DB"));

    Health health = underTest.check();

    verifyRedStatus(health);
  }

  /**
   * By contract {@link IsAliveMapper#isAlive()} can not return anything but 1. Still we write this test as a
   * protection against change in this contract.
   */
  @Test
  public void status_is_RED_with_single_cause_if_isAlive_does_not_return_1() {
    when(isAliveMapper.isAlive()).thenReturn(12);

    Health health = underTest.check();

    verifyRedStatus(health);
  }

  private void verifyRedStatus(Health health) {
    assertThat(health.getStatus()).isEqualTo(Health.Status.RED);
    assertThat(health.getCauses()).containsOnly("Can't connect to DB");
  }
}
