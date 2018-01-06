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

import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProxyClearCacheRequestBuilderTest {

  @ClassRule
  public static EsTester esTester = new EsTester();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void clear_cache() {
    ClearIndicesCacheRequestBuilder requestBuilder = esTester.client().prepareClearCache();
    requestBuilder.get();
  }

  @Test
  public void to_string() {
    assertThat(esTester.client().prepareClearCache().toString()).isEqualTo("ES clear cache request");
    assertThat(esTester.client().prepareClearCache("rules").toString()).isEqualTo("ES clear cache request on indices 'rules'");
    assertThat(esTester.client().prepareClearCache().setFields("key").toString()).isEqualTo("ES clear cache request on fields 'key'");
    assertThat(esTester.client().prepareClearCache().setFieldDataCache(true).toString()).isEqualTo("ES clear cache request with field data cache");
    assertThat(esTester.client().prepareClearCache().setRequestCache(true).toString()).isEqualTo("ES clear cache request with request cache");
  }

  @Test
  public void trace_logs() {
    logTester.setLevel(LoggerLevel.TRACE);
    ClearIndicesCacheRequestBuilder requestBuilder = esTester.client().prepareClearCache();
    requestBuilder.get();

    assertThat(logTester.logs()).hasSize(1);
  }

  @Test
  public void no_trace_logs() {
    logTester.setLevel(LoggerLevel.DEBUG);
    ClearIndicesCacheRequestBuilder requestBuilder = esTester.client().prepareClearCache();
    requestBuilder.get();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() {
    try {
      esTester.client().prepareClearCache().get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() {
    try {
      esTester.client().prepareClearCache().get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() {
    try {
      esTester.client().prepareClearCache().execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

}
