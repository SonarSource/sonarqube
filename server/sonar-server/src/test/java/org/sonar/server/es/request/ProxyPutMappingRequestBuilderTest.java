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
package org.sonar.server.es.request;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.FakeIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProxyPutMappingRequestBuilderTest {

  @Rule
  public EsTester esTester = new EsTester(new FakeIndexDefinition());

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void put_mapping() {
    PutMappingRequestBuilder requestBuilder = esTester.client().preparePutMapping(FakeIndexDefinition.INDEX)
      .setType(FakeIndexDefinition.TYPE)
      .setSource(mapDomain());
    requestBuilder.get();
  }

  @Test
  public void to_string() {
    assertThat(esTester.client().preparePutMapping(FakeIndexDefinition.INDEX).setSource(mapDomain()).toString())
      .isEqualTo("ES put mapping request on indices 'fakes' with source '{\"dynamic\":false,\"_all\":{\"enabled\":false}}'");
    assertThat(esTester.client().preparePutMapping(FakeIndexDefinition.INDEX).setType(FakeIndexDefinition.TYPE).setSource(mapDomain()).toString())
      .isEqualTo("ES put mapping request on indices 'fakes' on type 'fake' with source '{\"dynamic\":false,\"_all\":{\"enabled\":false}}'");
  }

  @Test
  public void trace_logs() {
    logTester.setLevel(LoggerLevel.TRACE);

    PutMappingRequestBuilder requestBuilder = esTester.client().preparePutMapping(FakeIndexDefinition.INDEX)
      .setType(FakeIndexDefinition.TYPE)
      .setSource(mapDomain());
    requestBuilder.get();

    assertThat(logTester.logs(LoggerLevel.TRACE)).hasSize(1);
  }

  @Test
  public void fail_on_bad_query() {
    try {
      esTester.client().preparePutMapping().get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES put mapping request");
    }
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() {
    try {
      esTester.client().preparePutMapping().get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() {
    try {
      esTester.client().preparePutMapping().get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() {
    try {
      esTester.client().preparePutMapping().execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

  protected static Map mapDomain() {
    Map<String, Object> mapping = new HashMap<>();
    mapping.put("dynamic", false);
    mapping.put("_all", ImmutableMap.of("enabled", false));
    return mapping;
  }

}
