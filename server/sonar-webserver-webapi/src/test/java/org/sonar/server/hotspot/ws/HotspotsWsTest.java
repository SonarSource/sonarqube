/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.hotspot.ws;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class HotspotsWsTest {

  @Test
  public void define_controller() {
    String[] actionKeys = IntStream.range(0, 1 + new Random().nextInt(12))
      .mapToObj(i -> i + randomAlphanumeric(10))
      .toArray(String[]::new);
    HotspotsWsAction[] actions = Arrays.stream(actionKeys)
      .map(actionKey -> new HotspotsWsAction() {
        @Override
        public void define(WebService.NewController context) {
          context.createAction(actionKey).setHandler(this);
        }

        @Override
        public void handle(Request request, Response response) {

        }
      })
      .toArray(HotspotsWsAction[]::new);
    WebService.Context context = new WebService.Context();
    new HotspotsWs(actions).define(context);
    WebService.Controller controller = context.controller("api/hotspots");

    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("8.1");
    assertThat(controller.actions()).extracting(WebService.Action::key).containsOnly(actionKeys);
  }

}
