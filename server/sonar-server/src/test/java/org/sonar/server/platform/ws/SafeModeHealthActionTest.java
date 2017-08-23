/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsActionTester;

public class SafeModeHealthActionTest {
  private AbstractHealthActionTestSupport healthActionTestSupport = new AbstractHealthActionTestSupport();
  private WsActionTester underTest = new WsActionTester(new SafeModeHealthAction(healthActionTestSupport.mockedHealthChecker));

  @Test
  public void verify_definition() {
    WebService.Action definition = underTest.getDef();

    healthActionTestSupport.verifyDefinition(definition);
  }

  @Test
  public void verify_example() {
    healthActionTestSupport.verifyExample(underTest);
  }

  @Test
  public void request_returns_status_and_causes_from_HealthChecker_check_method() {
    healthActionTestSupport.requestReturnsStatusAndCausesFromHealthCheckerCheckMethod(underTest);
  }

}
