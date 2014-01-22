/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.server.ws.SimpleRequest;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class RulesWsTest {

  WsTester tester = new WsTester(new RulesWs());

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/rules");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/rules");
    assertThat(controller.description()).isNotEmpty();

    WebService.Action search = controller.action("search");
    assertThat(search).isNotNull();
    assertThat(search.key()).isEqualTo("search");
    assertThat(search.handler()).isNotNull();
    assertThat(search.since()).isEqualTo("4.2");
    assertThat(search.isPost()).isFalse();
  }

  @Test
  public void search_for_rules() throws Exception {
    SimpleRequest request = new SimpleRequest();
    tester.execute("search", request).assertJson(getClass(), "search.json");
  }
}
