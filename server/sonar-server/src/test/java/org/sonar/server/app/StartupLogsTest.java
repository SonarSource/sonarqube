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
package org.sonar.server.app;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.utils.log.Logger;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StartupLogsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
  private Logger logger = mock(Logger.class);
  private TomcatStartupLogs underTest = new TomcatStartupLogs(logger);

  @Test
  public void fail_with_IAE_on_unsupported_protocol() {
    Connector connector = newConnector("AJP/1.3", "ajp");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported connector: Connector[AJP/1.3-1234]");
    
    underTest.log(tomcat);
  }

  @Test
  public void logHttp() {
    Connector connector = newConnector("HTTP/1.1", "http");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    underTest.log(tomcat);

    verify(logger).info("HTTP connector enabled on port 1234");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void unsupported_connector() {
    Connector connector = mock(Connector.class, Mockito.RETURNS_DEEP_STUBS);
    when(connector.getProtocol()).thenReturn("SPDY/1.1");
    when(connector.getScheme()).thenReturn("spdy");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});
    try {
      underTest.log(tomcat);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  private Connector newConnector(String protocol, String schema) {
    Connector httpConnector = new Connector(protocol);
    httpConnector.setScheme(schema);
    httpConnector.setPort(1234);
    return httpConnector;
  }
}
