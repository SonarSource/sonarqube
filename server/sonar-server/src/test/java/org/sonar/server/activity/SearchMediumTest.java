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
package org.sonar.server.activity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;

import java.lang.management.ManagementFactory;

public abstract class SearchMediumTest {


  private static Logger LOGGER = LoggerFactory.getLogger(SearchMediumTest.class);

  @ClassRule
  public static ServerTester tester = new ServerTester();

  protected DbClient db;
  protected IndexClient index;
  protected DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    index = tester.get(IndexClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);

    ManagementFactory.getMemoryMXBean();
    LOGGER.info("* Environment ({})", ManagementFactory.getOperatingSystemMXBean().getName());
    LOGGER.info("* heap:\t{}", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
    LOGGER.info("* #cpu:\t{}", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
    LOGGER.info("* load:\t{}", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
  }

  @After
  public void after() {
    if (dbSession != null) {
      dbSession.close();
    }
  }

}
