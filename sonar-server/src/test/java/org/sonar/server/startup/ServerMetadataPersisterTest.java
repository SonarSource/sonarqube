/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.platform.Server;
import org.sonar.server.platform.ServerImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ServerMetadataPersisterTest extends AbstractDbUnitTestCase {

  @Test
  public void testSaveProperties() throws ParseException {
    setupData("testSaveProperties");
    persist();
    checkTables("testSaveProperties", "properties");
  }

  @Test
  public void testUpdateExistingProperties() throws ParseException {
    setupData("testUpdateExistingProperties");
    persist();
    checkTables("testUpdateExistingProperties", "properties");
  }

  private void persist() throws ParseException {
    TimeZone initialZone = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
      Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2010-05-18 17:59");
      Server server = new ServerImpl("123", "2.2", date);
      ServerMetadataPersister persister = new ServerMetadataPersister(server, getSession());
      persister.start();
    } finally {
      TimeZone.setDefault(initialZone);
    }
  }
}
