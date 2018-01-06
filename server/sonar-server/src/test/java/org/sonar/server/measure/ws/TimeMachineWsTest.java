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
package org.sonar.server.measure.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeMachineWsTest {

  WebService.Controller controller;

  @Before
  public void setUp() {
    WsTester tester = new WsTester(new TimeMachineWs());
    controller = tester.controller("api/timemachine");
  }

  @Test
  public void define_controller() {
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("2.10");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(1);
  }

  @Test
  public void define_index_action() {
    WebService.Action action = controller.action("index");
    assertThat(action).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
  }

}
