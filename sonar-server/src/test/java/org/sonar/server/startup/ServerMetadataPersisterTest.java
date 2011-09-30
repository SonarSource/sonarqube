/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerMetadataPersisterTest extends AbstractDbUnitTestCase {

  private TimeZone initialTimeZone;

  @Before
  public void fixTimeZone() {
    initialTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  }

  @After
  public void revertTimeZone() {
    TimeZone.setDefault(initialTimeZone);
  }

  @Test
  public void testSaveProperties() throws ParseException {
    setupData("testSaveProperties");
    persist(newServer());
    checkTables("testSaveProperties", "properties");
  }

  @Test
  public void testUpdateExistingProperties() throws ParseException {
    setupData("testUpdateExistingProperties");
    persist(newServer());
    checkTables("testUpdateExistingProperties", "properties");
  }

  @Test
  public void testDeleteProperties() throws ParseException {
    setupData("testDeleteProperties");
    Server server = mock(Server.class);
    when(server.getStartedAt()).thenReturn(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2010-05-18 17:59"));//this is a mandatory not-null property
    persist(server);
    checkTables("testDeleteProperties", "properties");
  }

  private void persist(Server server) {
    ServerMetadataPersister persister = new ServerMetadataPersister(server, getSession());
    persister.start();
  }

  private Server newServer() throws ParseException {
    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2010-05-18 17:59");
    Server server = mock(Server.class);
    when(server.getPermanentServerId()).thenReturn("1abcdef");
    when(server.getId()).thenReturn("123");
    when(server.getVersion()).thenReturn("2.2");
    when(server.getStartedAt()).thenReturn(date);

    return server;

  }
}
