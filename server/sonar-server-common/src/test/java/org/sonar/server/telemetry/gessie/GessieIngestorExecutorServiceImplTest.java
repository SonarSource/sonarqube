/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.telemetry.gessie;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GessieIngestorExecutorServiceImplTest {

  @Test
  void constructor_shouldCreateReadyExecutor() {
    GessieIngestorExecutorServiceImpl underTest = new GessieIngestorExecutorServiceImpl();

    assertThat(underTest.isShutdown()).isFalse();
    assertThat(underTest.isTerminated()).isFalse();

    underTest.stop();
  }

  @Test
  void createThread_shouldCreateDaemonThreadWithIndexedName() {
    Thread t1 = GessieIngestorExecutorServiceImpl.createThread(() -> {});
    Thread t2 = GessieIngestorExecutorServiceImpl.createThread(() -> {});

    assertThat(t1.isDaemon()).isTrue();
    assertThat(t2.isDaemon()).isTrue();
    assertThat(t1.getName()).startsWith("gessie-ingestor-");
    assertThat(t2.getName()).startsWith("gessie-ingestor-");
    assertThat(t1.getName()).isNotEqualTo(t2.getName());
  }
}
