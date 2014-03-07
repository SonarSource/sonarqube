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
package org.sonar.wsclient.qualitygate.internal;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.qualitygate.*;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class DefaultQualityGateClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void should_create_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"id\":666,\"name\":\"Ninth\"}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    QualityGate result = client.create("Ninth");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/create");
    assertThat(httpServer.requestParams()).includes(
        entry("name", "Ninth")
        );
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(666L);
    assertThat(result.name()).isEqualTo("Ninth");
  }

  @Test
  public void should_list_qualitygates() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody(
        "{\"qualitygates\":[{\"id\":666,\"name\":\"Ninth\"},{\"id\":42,\"name\":\"Golden\"},{\"id\":43,\"name\":\"Star\"}]}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    QualityGates qGates = client.list();

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/list");
    assertThat(httpServer.requestParams()).isEmpty();
    assertThat(qGates.qualityGates()).hasSize(3);
    assertThat(qGates.defaultGate()).isNull();
  }

  @Test
  public void should_rename_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"id\":666,\"name\":\"Ninth\"}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    QualityGate result = client.rename(666L, "Hell");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/rename");
    assertThat(httpServer.requestParams()).includes(
        entry("id", "666"),
        entry("name", "Hell")
        );
    assertThat(result).isNotNull();
  }

  @Test
  public void should_show_qualitygate_by_id() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"id\":5,\"name\":\"Sonar way\",\"conditions\":["
      + "{\"id\":6,\"metric\":\"blocker_violations\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":7,\"metric\":\"critical_violations\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":10,\"metric\":\"test_errors\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":11,\"metric\":\"test_failures\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":12,\"metric\":\"new_coverage\",\"op\":\"LT\",\"warning\":\"\",\"error\":\"80%\",\"period\":3},"
      + "{\"id\":13,\"metric\":\"open_issues\",\"op\":\"GT\",\"warning\":\"0\",\"error\":\"\"},"
      + "{\"id\":14,\"metric\":\"reopened_issues\",\"op\":\"GT\",\"warning\":\"0\",\"error\":\"\"},"
      + "{\"id\":15,\"metric\":\"skipped_tests\",\"op\":\"GT\",\"warning\":\"0\",\"error\":\"\"}"
      + "]}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);

    QualityGateDetails qGate = client.show(5L);
    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/show?id=5");
    assertThat(qGate.id()).isEqualTo(5L);
    assertThat(qGate.name()).isEqualTo("Sonar way");

    Collection<QualityGateCondition> conditions = qGate.conditions();
    assertThat(conditions).hasSize(8);
    Iterator<QualityGateCondition> condIterator = conditions.iterator();
    QualityGateCondition first = condIterator.next();
    assertThat(first.id()).isEqualTo(6L);
    QualityGateCondition second = condIterator.next();
    assertThat(second.period()).isNull();
    QualityGateCondition third = condIterator.next();
    assertThat(third.metricKey()).isEqualTo("test_errors");
    QualityGateCondition fourth = condIterator.next();
    assertThat(fourth.operator()).isEqualTo("GT");
    QualityGateCondition fifth = condIterator.next();
    assertThat(fifth.errorThreshold()).isEqualTo("80%");
    assertThat(fifth.period()).isEqualTo(3);
    QualityGateCondition sixth = condIterator.next();
    assertThat(sixth.warningThreshold()).isEqualTo("0");
  }

  @Test
  public void should_show_qualitygate_by_name() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"id\":5,\"name\":\"Sonar way\",\"conditions\":["
      + "{\"id\":6,\"metric\":\"blocker_violations\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":7,\"metric\":\"critical_violations\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":10,\"metric\":\"test_errors\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":11,\"metric\":\"test_failures\",\"op\":\"GT\",\"warning\":\"\",\"error\":\"0\"},"
      + "{\"id\":12,\"metric\":\"new_coverage\",\"op\":\"LT\",\"warning\":\"\",\"error\":\"80%\",\"period\":3},"
      + "{\"id\":13,\"metric\":\"open_issues\",\"op\":\"GT\",\"warning\":\"0\",\"error\":\"\"},"
      + "{\"id\":14,\"metric\":\"reopened_issues\",\"op\":\"GT\",\"warning\":\"0\",\"error\":\"\"},"
      + "{\"id\":15,\"metric\":\"skipped_tests\",\"op\":\"GT\",\"warning\":\"0\",\"error\":\"\"}"
      + "]}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);

    QualityGateDetails qGate = client.show("Sonar way");
    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/show?name=Sonar%20way");
    assertThat(qGate.id()).isEqualTo(5L);
    assertThat(qGate.name()).isEqualTo("Sonar way");

    Collection<QualityGateCondition> conditions = qGate.conditions();
    assertThat(conditions).hasSize(8);
    Iterator<QualityGateCondition> condIterator = conditions.iterator();
    QualityGateCondition first = condIterator.next();
    assertThat(first.id()).isEqualTo(6L);
    QualityGateCondition second = condIterator.next();
    assertThat(second.period()).isNull();
    QualityGateCondition third = condIterator.next();
    assertThat(third.metricKey()).isEqualTo("test_errors");
    QualityGateCondition fourth = condIterator.next();
    assertThat(fourth.operator()).isEqualTo("GT");
    QualityGateCondition fifth = condIterator.next();
    assertThat(fifth.errorThreshold()).isEqualTo("80%");
    assertThat(fifth.period()).isEqualTo(3);
    QualityGateCondition sixth = condIterator.next();
    assertThat(sixth.warningThreshold()).isEqualTo("0");
  }

  @Test
  public void should_show_empty_qualitygate_by_id() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"id\":5,\"name\":\"Sonar way\"}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);

    QualityGateDetails qGate = client.show(5L);
    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/show?id=5");
    assertThat(qGate.id()).isEqualTo(5L);
    assertThat(qGate.name()).isEqualTo("Sonar way");

    Collection<QualityGateCondition> conditions = qGate.conditions();
    assertThat(conditions).isEmpty();
  }

  @Test
  public void should_destroy_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubStatusCode(HttpURLConnection.HTTP_NO_CONTENT);

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    client.destroy(666L);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/destroy");
    assertThat(httpServer.requestParams()).includes(
        entry("id", "666")
        );
  }

  @Test
  public void should_set_default_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubStatusCode(HttpURLConnection.HTTP_NO_CONTENT);

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    client.setDefault(666L);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/set_as_default");
    assertThat(httpServer.requestParams()).includes(
        entry("id", "666")
        );
  }

  @Test
  public void should_unset_default_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubStatusCode(HttpURLConnection.HTTP_NO_CONTENT);

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    client.unsetDefault();

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/unset_default");
    assertThat(httpServer.requestParams()).isEmpty();
  }
}
