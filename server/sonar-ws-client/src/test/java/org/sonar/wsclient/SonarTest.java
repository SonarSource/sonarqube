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
package org.sonar.wsclient;

import org.eclipse.jetty.testing.ServletTester;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.connectors.HttpClient3Connector;
import org.sonar.wsclient.connectors.HttpClient4Connector;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.Query;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertThat;

@RunWith(value = Parameterized.class)
public class SonarTest {

  private static ServletTester tester;
  private static String baseUrl;
  private Sonar sonar;

  public SonarTest(Sonar sonar) {
    this.sonar = sonar;
  }

  /**
   * This kicks off an instance of the Jetty
   * servlet container so that we can hit it.
   * We register an echo service that simply
   * returns the parameters passed to it.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() throws Exception {
    tester = new ServletTester();
    tester.setContextPath("/");
    tester.addServlet(ServerServlet.class, "/api/server/index");
    tester.addServlet(MetricServlet.class, "/api/metrics");
    tester.addServlet(EmptyServlet.class, "/api/empty");
    tester.addServlet(BadRulesServlet.class, "/api/rules");
    baseUrl = tester.createSocketConnector(true);
    tester.start();

    return Arrays.asList(new Object[][] {
      {new Sonar(new HttpClient4Connector(new Host(baseUrl)))},
      {new Sonar(new HttpClient3Connector(new Host(baseUrl)))}
    });
  }

  @AfterClass
  public static void stopServer() throws Exception {
    tester.stop();
  }

  @Test
  public void findAll() {
    Collection<Metric> metrics = sonar.findAll(MetricQuery.all());
    assertThat(metrics.size(), Matchers.greaterThan(1));
  }

  @Test
  public void findEmptyResults() {
    Query<Metric> query = new EmptyQuery();
    Collection<Metric> metrics = sonar.findAll(query);
    assertThat(metrics.size(), Matchers.is(0));
  }

  @Test
  public void returnNullWhenSingleResultNotFound() {
    Query<Metric> query = new EmptyQuery();
    assertThat(sonar.find(query), Matchers.nullValue());
  }

  @Test(expected = ConnectionException.class)
  public void failWhenConnectionIsClosed() throws Exception {
    Sonar fakeSonar = Sonar.create("http://localhost:70");
    fakeSonar.findAll(MetricQuery.all());
  }

  @Test
  public void getVersion() {
    Server server = sonar.find(new ServerQuery());
    assertThat(server.getId(), Matchers.is("123456789"));
    assertThat(server.getVersion(), Matchers.is("2.0"));
  }

  static class EmptyQuery extends Query<Metric> {
    @Override
    public String getUrl() {
      return "/api/empty";
    }

    @Override
    public Class<Metric> getModelClass() {
      return Metric.class;
    }
  }

}
