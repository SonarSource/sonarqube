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

package org.sonar.server.qualityprofile.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class QProfilesWsTest {

  @Mock
  QProfileBackupWsHandler qProfileBackupWsHandler;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QProfilesWs(qProfileBackupWsHandler));
  }

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/qprofiles");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/qprofiles");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(1);

    WebService.Action restoreProfiles = controller.action("restore_default");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.handler()).isNotNull();
    assertThat(restoreProfiles.since()).isEqualTo("4.4");
    assertThat(restoreProfiles.isPost()).isFalse();
    assertThat(restoreProfiles.isInternal()).isFalse();
    assertThat(restoreProfiles.params()).hasSize(1);
  }
}
