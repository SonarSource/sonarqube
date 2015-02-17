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

package org.sonar.server.es.request;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.FakeIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProxyGetRequestBuilderTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new FakeIndexDefinition());

  @Before
  public void setUp() throws Exception {
    esTester.setProfilingLevel(Profiling.Level.NONE);
  }

  @Test
  public void get() {
    esTester.client().prepareGet()
      .setIndex(FakeIndexDefinition.INDEX)
      .setType(FakeIndexDefinition.TYPE)
      .setId("ruleKey")
      .get();
  }

  @Test
  public void with_profiling_basic() {
    esTester.setProfilingLevel(Profiling.Level.FULL);

    esTester.client().prepareGet()
      .setIndex(FakeIndexDefinition.INDEX)
      .setType(FakeIndexDefinition.TYPE)
      .setId("ruleKey")
      .get();

    // TODO assert profiling
  }

  @Test
  public void fail_to_get_bad_query() throws Exception {
    GetRequestBuilder requestBuilder = esTester.client().prepareGet()
      .setIndex("unknown")
      .setType("test")
      .setId("rule1");
    try {
      requestBuilder.get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES get request for key 'rule1' on index 'unknown' on type 'test'");
    }
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    try {
      esTester.client().prepareGet().get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    try {
      esTester.client().prepareGet().get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() throws Exception {
    try {
      esTester.client().prepareGet().execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

}
