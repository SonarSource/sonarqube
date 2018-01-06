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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

public class MeasuresWsTest {
  WsTester ws = new WsTester(
    new MeasuresWs(
      new ComponentAction(null, null, null)));

  @Test
  public void define_ws() {
    WebService.Controller controller = ws.controller("api/measures");

    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("5.4");
  }
}
