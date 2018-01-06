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
package org.sonar.ce.taskprocessor;

import fi.iki.elonen.NanoHTTPD;
import org.junit.Test;
import org.sonar.ce.httpd.HttpAction;

import static fi.iki.elonen.NanoHTTPD.Method.GET;
import static fi.iki.elonen.NanoHTTPD.Method.POST;
import static fi.iki.elonen.NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.ce.httpd.CeHttpUtils.createHttpSession;

public class RefreshWorkerCountActionTest {
  private EnabledCeWorkerController enabledCeWorkerController = mock(EnabledCeWorkerController.class);
  private RefreshWorkerCountAction underTest = new RefreshWorkerCountAction(enabledCeWorkerController);

  @Test
  public void register_to_path_changeLogLevel() {
    HttpAction.ActionRegistry actionRegistry = mock(HttpAction.ActionRegistry.class);

    underTest.register(actionRegistry);

    verify(actionRegistry).register("refreshWorkerCount", underTest);
  }

  @Test
  public void serves_METHOD_NOT_ALLOWED_error_when_method_is_not_POST() {
    NanoHTTPD.Response response = underTest.serve(createHttpSession(GET));

    assertThat(response.getStatus()).isEqualTo(METHOD_NOT_ALLOWED);
    verifyZeroInteractions(enabledCeWorkerController);
  }

  @Test
  public void call_EnabledCeWorkerController_refresh_on_POST() {
    NanoHTTPD.Response response = underTest.serve(createHttpSession(POST));

    assertThat(response.getStatus()).isEqualTo(OK);
    verify(enabledCeWorkerController).refresh();
    verifyNoMoreInteractions(enabledCeWorkerController);
  }
}
