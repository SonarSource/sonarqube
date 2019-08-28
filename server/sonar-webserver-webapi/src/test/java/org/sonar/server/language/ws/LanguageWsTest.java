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
package org.sonar.server.language.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Controller;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageWsTest {

  private static final String CONTROLLER_LANGUAGES = "api/languages";
  private static final String ACTION_LIST = "list";

  private LanguageWs underTest = new LanguageWs(new ListAction(null));

  @Test
  public void should_be_well_defined() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    Controller controller = context.controller(CONTROLLER_LANGUAGES);
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.isInternal()).isFalse();
    assertThat(controller.path()).isEqualTo(CONTROLLER_LANGUAGES);
    assertThat(controller.since()).isEqualTo("5.1");
    assertThat(controller.actions()).hasSize(1);

    Action list = controller.action(ACTION_LIST);
    assertThat(list).isNotNull();
    assertThat(list.description()).isNotEmpty();
    assertThat(list.handler()).isInstanceOf(ListAction.class);
    assertThat(list.isInternal()).isFalse();
    assertThat(list.isPost()).isFalse();
    assertThat(list.responseExampleAsString()).isNotEmpty();
    assertThat(list.params()).hasSize(2);
  }
}
