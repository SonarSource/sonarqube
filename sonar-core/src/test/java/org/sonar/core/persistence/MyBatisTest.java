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
package org.sonar.core.persistence;

import ch.qos.logback.classic.Level;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.config.Logback;
import org.sonar.core.rule.RuleMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MyBatisTest {
  private static H2Database database;
  private Logback logback = mock(Logback.class);

  @BeforeClass
  public static void start() {
    database = new H2Database("sonar2", true);
    database.start();
  }

  @AfterClass
  public static void stop() {
    database.stop();
  }

  @Test
  public void shouldConfigureMyBatis() {
    MyBatis myBatis = new MyBatis(database, new Settings(), logback);
    myBatis.start();

    Configuration conf = myBatis.getSessionFactory().getConfiguration();
    assertThat(conf.isUseGeneratedKeys(), Is.is(true));
    assertThat(conf.hasMapper(RuleMapper.class), Is.is(true));
    assertThat(conf.isLazyLoadingEnabled(), Is.is(false));
  }

  @Test
  public void shouldOpenBatchSession() {
    MyBatis myBatis = new MyBatis(database, new Settings(), logback);
    myBatis.start();

    SqlSession session = myBatis.openBatchSession();
    try {
      assertThat(session.getConnection(), notNullValue());
      assertThat(session.getMapper(RuleMapper.class), notNullValue());
    } finally {
      session.close();
    }
  }

  @Test
  public void log_sql_requests_and_responses() {
    Settings settings = new Settings()
        .setProperty("sonar.log.profilingLevel", "FULL");

    MyBatis myBatis = new MyBatis(database, settings, logback);
    myBatis.start();

    verify(logback).setLoggerLevel("org.sonar.core.resource.ResourceIndexerMapper", Level.TRACE);
  }
}
