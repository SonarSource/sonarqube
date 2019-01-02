/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.batch.bootstrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingConfiguratorTest {
  private static final String DEFAULT_CLASSPATH_CONF = "/org/sonar/batch/bootstrapper/logback.xml";
  private static final String TEST_STR = "foo";
  private LoggingConfiguration conf = new LoggingConfiguration();
  private ByteArrayOutputStream out;
  private SimpleLogListener listener;
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() {
    out = new ByteArrayOutputStream();
    conf = new LoggingConfiguration();
    listener = new SimpleLogListener();
  }

  private class SimpleLogListener implements LogOutput {
    String msg;
    LogOutput.Level level;

    @Override
    public void log(String msg, LogOutput.Level level) {
      this.msg = msg;
      this.level = level;
    }
  }

  @Test
  public void testWithFile() throws FileNotFoundException, IOException {
    InputStream is = this.getClass().getResourceAsStream(DEFAULT_CLASSPATH_CONF);
    File tmpFolder = folder.getRoot();
    File testFile = new File(tmpFolder, "test");
    OutputStream os = new FileOutputStream(testFile);
    IOUtils.copy(is, os);
    os.close();

    conf.setLogOutput(listener);
    LoggingConfigurator.apply(conf, testFile);

    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info(TEST_STR);

    assertThat(listener.msg).endsWith(TEST_STR);
    assertThat(listener.level).isEqualTo(LogOutput.Level.INFO);
  }

  @Test
  public void testCustomAppender() throws UnsupportedEncodingException {
    conf.setLogOutput(listener);
    LoggingConfigurator.apply(conf);

    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info(TEST_STR);

    assertThat(listener.msg).endsWith(TEST_STR);
    assertThat(listener.level).isEqualTo(LogOutput.Level.INFO);
  }

  @Test
  public void testNoStdout() throws UnsupportedEncodingException {
    System.setOut(new PrintStream(out, false, StandardCharsets.UTF_8.name()));
    conf.setLogOutput(listener);
    LoggingConfigurator.apply(conf);

    Logger logger = LoggerFactory.getLogger(this.getClass());

    logger.error(TEST_STR);
    logger.info(TEST_STR);
    logger.debug(TEST_STR);
    assertThat(out.size()).isEqualTo(0);
  }

  @Test
  public void testConfigureMultipleTimes() throws UnsupportedEncodingException {
    System.setOut(new PrintStream(out, false, StandardCharsets.UTF_8.name()));
    conf.setLogOutput(listener);
    LoggingConfigurator.apply(conf);

    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.debug("debug");
    assertThat(listener.msg).isNull();

    conf.setVerbose(true);
    LoggingConfigurator.apply(conf);

    logger.debug("debug");
    assertThat(listener.msg).isEqualTo("debug");
  }

  @Test
  public void testFormatNoEffect() throws UnsupportedEncodingException {
    conf.setLogOutput(listener);
    conf.setFormat("%t");

    LoggingConfigurator.apply(conf);
    Logger logger = LoggerFactory.getLogger(this.getClass());

    logger.info("info");

    assertThat(listener.msg).isEqualTo("info");
  }

  @Test
  public void testNoListener() throws UnsupportedEncodingException {
    System.setOut(new PrintStream(out, false, StandardCharsets.UTF_8.name()));
    LoggingConfigurator.apply(conf);

    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("info");

    assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).contains("info");
  }

}
