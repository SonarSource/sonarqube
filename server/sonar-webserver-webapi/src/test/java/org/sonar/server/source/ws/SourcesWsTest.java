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
package org.sonar.server.source.ws;

import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class SourcesWsTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Test
  public void define_ws() {
    SourcesWsAction[] actions = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> {
        SourcesWsAction wsAction = mock(SourcesWsAction.class);
        doAnswer(invocation -> {
          WebService.NewController controller = invocation.getArgument(0);
          controller.createAction("action_" + i)
            .setHandler(wsAction);
          return null;
        }).when(wsAction).define(any(WebService.NewController.class));
        return wsAction;
      })
      .toArray(SourcesWsAction[]::new);

    SourcesWs underTest = new SourcesWs(actions);
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/sources");
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("4.2");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(actions.length);
  }
}
