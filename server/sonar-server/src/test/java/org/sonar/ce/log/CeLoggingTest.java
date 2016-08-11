/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.MDC;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.log.CeLogging.MDC_CE_ACTIVITY_FLAG;

public class CeLoggingTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private LogbackHelper helper = new LogbackHelper();
  private File logDir;

  @Before
  public void setUp() throws Exception {
    this.logDir = temp.newFolder();
  }

  @After
  public void resetLogback() throws JoranException {
    helper.resetFromXml("/logback-test.xml");
  }

  @After
  public void cleanMDC() throws Exception {
    MDC.clear();
  }

  @Test
  public void createCeConfigurationConfiguration_fails_if_log_directory_is_not_set_in_Props() {
    LogbackHelper helper = new LogbackHelper();
    LoggerContext ctx = new LoggerContext();
    Props processProps = new Props(new Properties());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property sonar.path.logs is not set");

    CeLogging.createCeActivityAppenderConfiguration(helper, ctx, processProps);
  }

  @Test
  public void createCeConfigurationConfiguration() {
    Properties props = new Properties();
    props.setProperty(ProcessProperties.PATH_LOGS, logDir.getAbsolutePath());
    Appender<ILoggingEvent> appender = CeLogging.createCeActivityAppenderConfiguration(new LogbackHelper(), new LoggerContext(), new Props(props));

    // filter on CE logs
    List<Filter<ILoggingEvent>> filters = appender.getCopyOfAttachedFiltersList();
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0)).isInstanceOf(CeActivityLogAcceptFilter.class);

    assertThat(appender).isInstanceOf(FileAppender.class);
    assertThat(appender.getName()).isEqualTo("ce_activity");
    FileAppender fileAppender = (FileAppender) appender;
    assertThat(fileAppender.getEncoder())
      .isInstanceOf(PatternLayoutEncoder.class);
    assertThat(fileAppender.getFile()).isEqualTo(new File(logDir, "ce_activity.log").getAbsolutePath());
  }

  @Test
  public void logCeActivity_of_Runnable_set_flag_in_MDC_calls_Runnable_and_remove_flag() {
    AtomicBoolean called = new AtomicBoolean(false);
    CeLogging.logCeActivity(() -> {
      assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isEqualTo("true");
      called.compareAndSet(false, true);
    });
    assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isNull();
    assertThat(called.get()).isTrue();
  }

  @Test
  public void logCeActivity_of_Runnable_set_flag_in_MDC_calls_Supplier_and_remove_flag() {
    AtomicBoolean called = new AtomicBoolean(false);
    CeLogging.logCeActivity(() -> {
      assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isEqualTo("true");
      called.compareAndSet(false, true);
      return 1;
    });
    assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isNull();
    assertThat(called.get()).isTrue();
  }

}
