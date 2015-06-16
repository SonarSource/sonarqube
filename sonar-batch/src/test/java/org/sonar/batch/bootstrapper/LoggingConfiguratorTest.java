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
package org.sonar.batch.bootstrapper;

import org.sonar.home.log.LogListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.Before;

public class LoggingConfiguratorTest {
  private static final String TEST_STR = "foo";
  private LoggingConfiguration conf = new LoggingConfiguration();
  private ByteArrayOutputStream out;
  private SimpleLogListener listener;

  @Before
  public void setUp() {
    out = new ByteArrayOutputStream();
    conf = new LoggingConfiguration();
    listener = new SimpleLogListener();
  }

  private class SimpleLogListener implements LogListener {
    String msg;
    Level level;

    @Override
    public void log(String msg, Level level) {
      this.msg = msg;
      this.level = level;
    }
  }

  @Test
  public void testCustomAppender() throws UnsupportedEncodingException {
    conf.setListener(listener);
    LoggingConfigurator.apply(conf);

    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info(TEST_STR);

    assertThat(listener.msg).endsWith(TEST_STR);
    assertThat(listener.level).isEqualTo(LogListener.Level.INFO);
  }

  @Test
  public void testNoStdout() throws UnsupportedEncodingException {
    System.setOut(new PrintStream(out, false, StandardCharsets.UTF_8.name()));
    conf.setListener(listener);
    LoggingConfigurator.apply(conf);

    Logger logger = LoggerFactory.getLogger(this.getClass());

    logger.error(TEST_STR);
    logger.info(TEST_STR);
    logger.debug(TEST_STR);
    assertThat(out.size()).isEqualTo(0);
  }

  @Test
  public void testFormatNoEffect() throws UnsupportedEncodingException {
    conf.setListener(listener);
    conf.setFormat("%t");

    LoggingConfigurator.apply(conf);
    Logger logger = LoggerFactory.getLogger(this.getClass());

    logger.info("info");

    assertThat(listener.msg).isEqualTo("info");
  }

  @Test
  public void testSqlClasspath() throws UnsupportedEncodingException {
    String classpath = "/org/sonar/batch/bootstrapper/logback.xml";

    conf.setListener(listener);
    conf.setShowSql(true);

    LoggingConfigurator.apply(conf, classpath);

    Logger logger = LoggerFactory.getLogger("java.sql");
    logger.info("foo");

    assertThat(listener.msg).endsWith(TEST_STR);
  }

  @Test
  public void testNoListener() throws UnsupportedEncodingException {
    System.setOut(new PrintStream(out, false, StandardCharsets.UTF_8.name()));
    LoggingConfigurator.apply(conf);

    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("info");

    assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).contains("info");
  }

  @Test
  public void testNoSqlClasspath() throws UnsupportedEncodingException {
    String classpath = "/org/sonar/batch/bootstrapper/logback.xml";

    conf.setListener(listener);
    conf.setShowSql(false);

    LoggingConfigurator.apply(conf, classpath);

    Logger logger = LoggerFactory.getLogger("java.sql");
    logger.info("foo");

    assertThat(listener.msg).isNull();
  }
}
