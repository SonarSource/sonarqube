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

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.FakeIndexDefinition;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProxyIndexRequestBuilderTest {

  @Rule
  public EsTester esTester = new EsTester().addDefinitions(new FakeIndexDefinition());

  @Test
  public void index_with_index_type_and_id() {
    esTester.setProfilingLevel(Profiling.Level.FULL);
    IndexResponse response = esTester.client().prepareIndex(FakeIndexDefinition.INDEX, FakeIndexDefinition.TYPE)
      .setSource(FakeIndexDefinition.newDoc(42))
      .get();
    assertThat(response.isCreated()).isTrue();
  }

  @Test
  public void with_profiling_basic() {
    esTester.setProfilingLevel(Profiling.Level.BASIC);
    IndexResponse response = esTester.client().prepareIndex(FakeIndexDefinition.INDEX, FakeIndexDefinition.TYPE)
      .setSource(FakeIndexDefinition.newDoc(42))
      .get();
    assertThat(response.isCreated()).isTrue();
    // TODO assert profiling
  }

  @Test
  public void fail_if_bad_query() throws Exception {
    esTester.setProfilingLevel(Profiling.Level.FULL);
    IndexRequestBuilder requestBuilder = esTester.client().prepareIndex("unknownIndex", "unknownType");
    try {
      requestBuilder.get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES index request for key 'null' on index 'unknownIndex' on type 'unknownType'");
    }
  }

  @Test
  public void fail_if_bad_query_with_basic_profiling() throws Exception {
    esTester.setProfilingLevel(Profiling.Level.BASIC);
    IndexRequestBuilder requestBuilder = esTester.client().prepareIndex("unknownIndex", "unknownType");
    try {
      requestBuilder.get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES index request for key 'null' on index 'unknownIndex' on type 'unknownType'");
    }
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    esTester.setProfilingLevel(Profiling.Level.FULL);
    try {
      esTester.client().prepareIndex(FakeIndexDefinition.INDEX, FakeIndexDefinition.TYPE).get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    esTester.setProfilingLevel(Profiling.Level.FULL);
    try {
      esTester.client().prepareIndex(FakeIndexDefinition.INDEX, FakeIndexDefinition.TYPE).get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void do_not_support_execute_method() throws Exception {
    esTester.setProfilingLevel(Profiling.Level.FULL);
    try {
      esTester.client().prepareIndex(FakeIndexDefinition.INDEX, FakeIndexDefinition.TYPE).execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

}
