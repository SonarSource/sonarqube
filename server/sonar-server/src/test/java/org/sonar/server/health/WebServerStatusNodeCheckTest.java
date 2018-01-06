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

import java.util.Arrays;
import java.util.Random;
import org.junit.Test;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebServerStatusNodeCheckTest {
  private final DatabaseMigrationState migrationState = mock(DatabaseMigrationState.class);
  private final Platform platform = mock(Platform.class);
  private final RestartFlagHolder restartFlagHolder = mock(RestartFlagHolder.class);

  private final Random random = new Random();

  private WebServerStatusNodeCheck underTest = new WebServerStatusNodeCheck(migrationState, platform, restartFlagHolder);

  @Test
  public void returns_RED_status_with_cause_if_platform_status_is_not_UP() {
    Platform.Status[] statusesButUp = Arrays.stream(Platform.Status.values())
      .filter(s -> s != Platform.Status.UP)
      .toArray(Platform.Status[]::new);
    Platform.Status randomStatusButUp = statusesButUp[random.nextInt(statusesButUp.length)];
    when(platform.status()).thenReturn(randomStatusButUp);

    Health health = underTest.check();

    verifyRedHealthWithCause(health);
  }

  @Test
  public void returns_RED_status_with_cause_if_platform_status_is_UP_but_migrationStatus_is_neither_NONE_nor_SUCCEED() {
    when(platform.status()).thenReturn(Platform.Status.UP);
    DatabaseMigrationState.Status[] statusesButValidOnes = Arrays.stream(DatabaseMigrationState.Status.values())
      .filter(s -> s != DatabaseMigrationState.Status.NONE)
      .filter(s -> s != DatabaseMigrationState.Status.SUCCEEDED)
      .toArray(DatabaseMigrationState.Status[]::new);
    DatabaseMigrationState.Status randomInvalidStatus = statusesButValidOnes[random.nextInt(statusesButValidOnes.length)];
    when(migrationState.getStatus()).thenReturn(randomInvalidStatus);

    Health health = underTest.check();

    verifyRedHealthWithCause(health);
  }

  @Test
  public void returns_RED_with_cause_if_platform_status_is_UP_migration_status_is_valid_but_SQ_is_restarting() {
    when(platform.status()).thenReturn(Platform.Status.UP);
    when(migrationState.getStatus()).thenReturn(random.nextBoolean() ? DatabaseMigrationState.Status.NONE : DatabaseMigrationState.Status.SUCCEEDED);
    when(restartFlagHolder.isRestarting()).thenReturn(true);

    Health health = underTest.check();

    verifyRedHealthWithCause(health);
  }

  @Test
  public void returns_GREEN_without_cause_if_platform_status_is_UP_migration_status_is_valid_and_SQ_is_not_restarting() {
    when(platform.status()).thenReturn(Platform.Status.UP);
    when(migrationState.getStatus()).thenReturn(random.nextBoolean() ? DatabaseMigrationState.Status.NONE : DatabaseMigrationState.Status.SUCCEEDED);
    when(restartFlagHolder.isRestarting()).thenReturn(false);

    Health health = underTest.check();

    assertThat(health).isEqualTo(Health.GREEN);
  }

  private void verifyRedHealthWithCause(Health health) {
    assertThat(health.getStatus()).isEqualTo(Health.Status.RED);
    assertThat(health.getCauses()).containsOnly("SonarQube webserver is not up");
  }
}
