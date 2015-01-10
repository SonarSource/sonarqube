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
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ProfilesWsTest {

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new ProfilesWs());
  }

  @Test
  public void define_controller() throws Exception {
    WebService.Controller controller = tester.controller("api/profiles");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/profiles");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(6);
  }

  @Test
  public void define_list_action() throws Exception {
    WebService.Controller controller = tester.controller("api/profiles");

    WebService.Action restoreProfiles = controller.action("list");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.handler()).isInstanceOf(RailsHandler.class);
    assertThat(restoreProfiles.responseExampleAsString()).isNotEmpty();
    assertThat(restoreProfiles.params()).hasSize(3);
  }

  @Test
  public void define_index_action() throws Exception {
    WebService.Controller controller = tester.controller("api/profiles");

    WebService.Action restoreProfiles = controller.action("index");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.handler()).isInstanceOf(RailsHandler.class);
    assertThat(restoreProfiles.responseExampleAsString()).isNotEmpty();
    assertThat(restoreProfiles.params()).hasSize(3);
  }

  @Test
  public void define_backup_action() throws Exception {
    WebService.Controller controller = tester.controller("api/profiles");

    WebService.Action restoreProfiles = controller.action("backup");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.handler()).isInstanceOf(RailsHandler.class);
    assertThat(restoreProfiles.params()).hasSize(3);
  }

  @Test
  public void define_restore_action() throws Exception {
    WebService.Controller controller = tester.controller("api/profiles");

    WebService.Action restoreProfiles = controller.action("restore");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.handler()).isInstanceOf(RailsHandler.class);
    assertThat(restoreProfiles.params()).hasSize(2);
  }

  @Test
  public void define_destroy_action() throws Exception {
    WebService.Controller controller = tester.controller("api/profiles");

    WebService.Action restoreProfiles = controller.action("destroy");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.handler()).isInstanceOf(RailsHandler.class);
    assertThat(restoreProfiles.params()).hasSize(2);
  }

  @Test
  public void define_set_as_default_action() throws Exception {
    WebService.Controller controller = tester.controller("api/profiles");

    WebService.Action restoreProfiles = controller.action("set_as_default");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.handler()).isInstanceOf(RailsHandler.class);
    assertThat(restoreProfiles.params()).hasSize(2);
  }
}
