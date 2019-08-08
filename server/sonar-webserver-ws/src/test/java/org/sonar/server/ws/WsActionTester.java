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
package org.sonar.server.ws;

import com.google.common.collect.Iterables;
import org.sonar.api.server.ws.WebService;

public class WsActionTester {

  public static final String CONTROLLER_KEY = "test";
  private final WebService.Action action;

  public WsActionTester(WsAction wsAction) {
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(CONTROLLER_KEY);
    wsAction.define(newController);
    newController.done();
    action = Iterables.get(context.controller(CONTROLLER_KEY).actions(), 0);
  }

  public WebService.Action getDef() {
    return action;
  }

  public TestRequest newRequest() {
    TestRequest request = new TestRequest();
    request.setAction(action);
    return request;
  }
}
