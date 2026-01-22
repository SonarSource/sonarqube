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
package org.sonar.server.platform.ws;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.HazelcastMember;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.cluster.hz.HazelcastObjects.LOG_LEVEL_KEY;
import static org.sonar.process.cluster.hz.HazelcastObjects.RUNTIME_CONFIG;

public class ChangeLogLevelClusterServiceTest {

  private final HazelcastMember hazelcastMember = mock();
  private final ChangeLogLevelClusterService underTest = new ChangeLogLevelClusterService(hazelcastMember);
  private Map<Object, Object> runtimeConfig;

  @Before
  public void setUp() throws InterruptedException {
    runtimeConfig = new HashMap<>();
    when(hazelcastMember.getReplicatedMap(RUNTIME_CONFIG)).thenReturn(runtimeConfig);

    DistributedAnswer<Object> answer = mock();
    when(hazelcastMember.call(any(), any(), anyLong())).thenReturn(answer);
  }

  @Test
  public void changeLogLevel_shouldStoreLogLevelInHazelcast() throws InterruptedException {
    underTest.changeLogLevel(LoggerLevel.DEBUG);

    assertThat(runtimeConfig).containsEntry(LOG_LEVEL_KEY, "DEBUG");
  }

  @Test
  public void changeLogLevel_shouldCallDistributedChangeLogLevel() throws InterruptedException {
    DistributedAnswer<Object> answer = mock();
    when(hazelcastMember.call(any(), any(), anyLong())).thenReturn(answer);

    underTest.changeLogLevel(LoggerLevel.DEBUG);

    verify(hazelcastMember).call(any(), any(), eq(5000L));
    verify(answer).propagateExceptions();
  }

  @Test
  public void changeLogLevel_shouldOverwritePreviousLogLevel() throws InterruptedException {
    underTest.changeLogLevel(LoggerLevel.DEBUG);
    assertThat(runtimeConfig).containsEntry(LOG_LEVEL_KEY, "DEBUG");

    underTest.changeLogLevel(LoggerLevel.INFO);
    assertThat(runtimeConfig).containsEntry(LOG_LEVEL_KEY, "INFO");
  }
}
