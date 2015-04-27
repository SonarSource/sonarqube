/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.source.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SourcesWsTest {

  ShowAction showAction = new ShowAction(mock(SourceService.class), mock(DbClient.class));
  RawAction rawAction = new RawAction(mock(DbClient.class), mock(SourceService.class));
  LinesAction linesAction = new LinesAction(mock(DbClient.class), mock(SourceLineIndex.class), mock(HtmlSourceDecorator.class));
  HashAction hashAction = new HashAction(mock(DbClient.class));
  IndexAction indexAction = new IndexAction(mock(DbClient.class), mock(SourceService.class));
  WsTester tester = new WsTester(new SourcesWs(showAction, rawAction, linesAction, hashAction, indexAction));

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/sources");
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("4.2");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(5);

    WebService.Action show = controller.action("show");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isSameAs(showAction);
    assertThat(show.since()).isEqualTo("4.4");
    assertThat(show.isInternal()).isFalse();
    assertThat(show.responseExampleAsString()).isNotEmpty();
    assertThat(show.params()).hasSize(3);

    WebService.Action raw = controller.action("raw");
    assertThat(raw).isNotNull();
    assertThat(raw.handler()).isSameAs(rawAction);
    assertThat(raw.since()).isEqualTo("5.0");
    assertThat(raw.isInternal()).isFalse();
    assertThat(raw.responseExampleAsString()).isNotEmpty();
    assertThat(raw.params()).hasSize(1);

    WebService.Action lines = controller.action("lines");
    assertThat(lines).isNotNull();
    assertThat(lines.handler()).isSameAs(linesAction);
    assertThat(lines.since()).isEqualTo("5.0");
    assertThat(lines.isInternal()).isTrue();
    assertThat(lines.responseExampleAsString()).isNotEmpty();
    assertThat(lines.params()).hasSize(4);

    WebService.Action hash = controller.action("hash");
    assertThat(hash).isNotNull();
    assertThat(hash.handler()).isSameAs(hashAction);
    assertThat(hash.since()).isEqualTo("5.0");
    assertThat(hash.isInternal()).isTrue();
    assertThat(hash.responseExampleAsString()).isNotEmpty();
    assertThat(hash.params()).hasSize(1);

    WebService.Action index = controller.action("index");
    assertThat(index).isNotNull();
    assertThat(index.handler()).isSameAs(indexAction);
    assertThat(index.since()).isEqualTo("5.0");
    assertThat(index.isInternal()).isTrue();
    assertThat(index.responseExampleAsString()).isNotEmpty();
    assertThat(index.params()).hasSize(3);

  }
}
