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
package org.sonar.server.qualitygate.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WsTester;
import org.sonar.server.qualitygate.QualityGates;

@RunWith(MockitoJUnitRunner.class)
public class QualityGatesWsTest {

  @Mock
  private QualityGates qGates;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QualityGatesWs(qGates));
  }

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/qualitygates");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/qualitygates");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(2);

    WebService.Action list = controller.action("list");
    assertThat(list).isNotNull();
    assertThat(list.handler()).isNotNull();
    assertThat(list.since()).isEqualTo("4.3");
    assertThat(list.isPost()).isFalse();
    assertThat(list.isPrivate()).isFalse();

    WebService.Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.handler()).isNotNull();
    assertThat(create.since()).isEqualTo("4.3");
    assertThat(create.isPost()).isTrue();
    assertThat(create.params()).hasSize(1);
    assertThat(create.param("name")).isNotNull();
    assertThat(create.isPrivate()).isFalse();
  }
}
