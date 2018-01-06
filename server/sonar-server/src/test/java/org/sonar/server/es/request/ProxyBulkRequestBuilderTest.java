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

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.FakeIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyBulkRequestBuilderTest {

  @Rule
  public EsTester esTester = new EsTester(new FakeIndexDefinition());

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void no_trace_logs() {
    logTester.setLevel(LoggerLevel.INFO);
    testBulk();
    assertThat(logTester.logs(LoggerLevel.TRACE)).isEmpty();
  }

  @Test
  public void trace_logs() {
    logTester.setLevel(LoggerLevel.TRACE);
    testBulk();
    assertThat(logTester.logs(LoggerLevel.TRACE)).hasSize(1);
  }

  private void testBulk() {
    BulkRequestBuilder req = esTester.client().prepareBulk();
    req.add(new UpdateRequest(FakeIndexDefinition.INDEX, FakeIndexDefinition.INDEX_TYPE_FAKE.getType(), "key1")
      .doc(FakeIndexDefinition.newDoc(1).getFields()));
    req.add(new DeleteRequest(FakeIndexDefinition.INDEX, FakeIndexDefinition.INDEX_TYPE_FAKE.getType(), "key2"));
    req.add(new IndexRequest(FakeIndexDefinition.INDEX, FakeIndexDefinition.INDEX_TYPE_FAKE.getType(), "key3")
      .source(FakeIndexDefinition.newDoc(3).getFields()));

    assertThat(req.toString()).isEqualTo(
      "Bulk[1 update request(s) on index fakes and type fake, 1 delete request(s) on index fakes and type fake, 1 index request(s) on index fakes and type fake]");

    BulkResponse response = req.get();
    assertThat(response.getItems()).hasSize(3);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void get_with_string_timeout_is_not_yet_implemented() {
    esTester.client().prepareBulk().get("1");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void get_with_time_value_timeout_is_not_yet_implemented() {
    esTester.client().prepareBulk().get(TimeValue.timeValueMinutes(1));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void execute_is_not_yet_implemented() {
    esTester.client().prepareBulk().execute();
  }

}
