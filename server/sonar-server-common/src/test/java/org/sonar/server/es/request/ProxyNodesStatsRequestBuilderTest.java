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
package org.sonar.server.es.request;

import org.elasticsearch.common.unit.TimeValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.newindex.FakeIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyNodesStatsRequestBuilderTest {

  @Rule
  public EsTester es = EsTester.createCustom();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void stats() {
    es.client().prepareNodesStats().get();
  }

  @Test
  public void to_string() {
    assertThat(es.client().prepareNodesStats().setNodesIds("node1").toString()).isEqualTo("ES nodes stats request on nodes 'node1'");
    assertThat(es.client().prepareNodesStats().toString()).isEqualTo("ES nodes stats request");
  }

  @Test
  public void trace_logs() {
    logTester.setLevel(LoggerLevel.TRACE);

    es.client().prepareNodesStats().get();

    assertThat(logTester.logs()).hasSize(1);
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Not yet implemented");

    es.client().prepareNodesStats(FakeIndexDefinition.INDEX).get("1");
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Not yet implemented");

    es.client().prepareNodesStats(FakeIndexDefinition.INDEX).get(TimeValue.timeValueMinutes(1));
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("execute() should not be called as it's used for asynchronous");

    es.client().prepareNodesStats(FakeIndexDefinition.INDEX).execute();
  }

}
