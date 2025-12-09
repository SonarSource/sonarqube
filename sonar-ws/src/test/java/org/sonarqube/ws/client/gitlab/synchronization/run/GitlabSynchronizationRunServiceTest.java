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
package org.sonarqube.ws.client.gitlab.synchronization.run;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

@RunWith(MockitoJUnitRunner.class)
public class GitlabSynchronizationRunServiceTest {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private WsConnector wsConnector;

  @InjectMocks
  private GitlabSynchronizationRunService gitlabSynchronizationRunService;

  @Test
  public void triggerRun_whenTriggered_shouldNotFail() {
    assertThatNoException().isThrownBy(() ->gitlabSynchronizationRunService.triggerRun());
  }

}
