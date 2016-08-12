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

import ch.qos.logback.classic.Level;
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
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.sonar.api.utils.log.Logger;
import org.sonar.ce.queue.CeTask;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.log.CeLogging.MDC_CE_ACTIVITY_FLAG;
import static org.sonar.ce.log.CeLogging.MDC_CE_TASK_UUID;

public class CeLoggingTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private LogbackHelper helper = new LogbackHelper();
  private File logDir;

  private CeLogging underTest = new CeLogging();

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
  public void initForTask_stores_task_uuid_in_MDC() {
    String uuid = "ce_task_uuid";

    underTest.initForTask(createCeTask(uuid));

    assertThat(MDC.get(MDC_CE_TASK_UUID)).isEqualTo(uuid);
  }

  private CeTask createCeTask(String uuid) {
    CeTask ceTask = Mockito.mock(CeTask.class);
    when(ceTask.getUuid()).thenReturn(uuid);
    return ceTask;
  }

  @Test
  public void clearForTask_removes_task_uuid_from_MDC() {
    MDC.put(MDC_CE_TASK_UUID, "some_value");

    underTest.clearForTask();

    assertThat(MDC.get(MDC_CE_TASK_UUID)).isNull();
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
  public void logCeActivity_of_Runnable_set_flag_in_MDC_to_ce_only_when_log_level_is_ERROR_and_calls_Runnable_and_remove_flag_even_if_Runnable_throws_exception() {
    callLogCeActivityOfRunnableAndVerify("ce_only", ERROR);
    callLogCeActivityOfFailingRunnableAndVerify("ce_only", ERROR);
  }

  @Test
  public void logCeActivity_of_Runnable_set_flag_in_MDC_to_ce_only_when_log_level_is_WARN_and_calls_Runnable_and_remove_flag_even_if_Runnable_throws_exception() {
    callLogCeActivityOfRunnableAndVerify("ce_only", WARN);
    callLogCeActivityOfFailingRunnableAndVerify("ce_only", WARN);
  }

  @Test
  public void logCeActivity_of_Runnable_set_flag_in_MDC_to_ce_only_when_log_level_is_INFO_and_calls_Runnable_and_remove_flag_even_if_Runnable_throws_exception() {
    callLogCeActivityOfRunnableAndVerify("ce_only", INFO);
  }

  @Test
  public void logCeActivity_of_Runnable_set_flag_in_MDC_to_all_when_log_level_is_DEBUG_and_calls_Runnable_and_remove_flag_even_if_Runnable_throws_exception() {
    callLogCeActivityOfRunnableAndVerify("all", DEBUG);
    callLogCeActivityOfFailingRunnableAndVerify("all", DEBUG);
  }

  @Test
  public void logCeActivity_of_Runnable_set_flag_in_MDC_to_all_when_log_level_is_TRACE_and_calls_Runnable_and_remove_flag_even_if_Runnable_throws_exception() {
    callLogCeActivityOfRunnableAndVerify("all", TRACE);
    callLogCeActivityOfFailingRunnableAndVerify("all", TRACE);
  }

  private void callLogCeActivityOfRunnableAndVerify(String expectedMdcValue, Level logLevel) {
    AtomicBoolean called = new AtomicBoolean(false);
    underTest.logCeActivity(
      createLogger(logLevel),
      () -> {
        assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isEqualTo(expectedMdcValue);
        called.compareAndSet(false, true);
      });
    assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isNull();
    assertThat(called.get()).isTrue();
  }

  private void callLogCeActivityOfFailingRunnableAndVerify(String expectedMdcValue, Level logLevel) {
    RuntimeException exception = new RuntimeException("Simulates a failing Runnable");

    AtomicBoolean called = new AtomicBoolean(false);
    try {
      underTest.logCeActivity(
        createLogger(logLevel),
        () -> {
          assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isEqualTo(expectedMdcValue);
          called.compareAndSet(false, true);
          throw exception;
        });
      fail("exception should have been raised");
    } catch (Exception e) {
      assertThat(e).isSameAs(exception);
    } finally {
      assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isNull();
      assertThat(called.get()).isTrue();
    }
  }

  private static Logger createLogger(Level info) {
    Logger logger = mock(Logger.class);
    when(logger.isDebugEnabled()).thenReturn(DEBUG.isGreaterOrEqual(info));
    when(logger.isTraceEnabled()).thenReturn(TRACE.isGreaterOrEqual(info));
    return logger;
  }

  @Test
  public void logCeActivity_of_Supplier_set_flag_in_MDC_to_ce_only_when_log_level_is_ERROR_and_calls_Supplier_and_remove_flag_even_if_Supplier_throws_exception() {
    callLogCeActivityOfSupplierAndVerify(ERROR, "ce_only");
    callLogCeActivityOfFailingSupplierAndVerify(ERROR, "ce_only");
  }

  @Test
  public void logCeActivity_of_Supplier_set_flag_in_MDC_to_ce_only_when_log_level_is_WARN_and_calls_Supplier_and_remove_flag_even_if_Supplier_throws_exception() {
    callLogCeActivityOfSupplierAndVerify(WARN, "ce_only");
    callLogCeActivityOfFailingSupplierAndVerify(WARN, "ce_only");
  }

  @Test
  public void logCeActivity_of_Supplier_set_flag_in_MDC_to_ce_only_when_log_level_is_INFO_and_calls_Supplier_and_remove_flag_even_if_Supplier_throws_exception() {
    callLogCeActivityOfSupplierAndVerify(INFO, "ce_only");
    callLogCeActivityOfFailingSupplierAndVerify(INFO, "ce_only");
  }

  @Test
  public void logCeActivity_of_Supplier_set_flag_in_MDC_to_all_when_log_level_is_DEBUG_and_calls_Supplier_and_remove_flag_even_if_Supplier_throws_exception() {
    callLogCeActivityOfSupplierAndVerify(DEBUG, "all");
    callLogCeActivityOfFailingSupplierAndVerify(DEBUG, "all");
  }

  @Test
  public void logCeActivity_of_Supplier_set_flag_in_MDC_to_all_when_log_level_is_TRACE_and_calls_Supplier_and_remove_flag_even_if_Supplier_throws_exception() {
    callLogCeActivityOfSupplierAndVerify(TRACE, "all");
    callLogCeActivityOfFailingSupplierAndVerify(TRACE, "all");
  }

  private void callLogCeActivityOfSupplierAndVerify(Level logLevel, String expectedFlag) {
    AtomicBoolean called = new AtomicBoolean(false);
    underTest.logCeActivity(
      createLogger(logLevel),
      () -> {
        assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isEqualTo(expectedFlag);
        called.compareAndSet(false, true);
        return 1;
      });
    assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isNull();
    assertThat(called.get()).isTrue();
  }

  private void callLogCeActivityOfFailingSupplierAndVerify(Level logLevel, String expectedFlag) {
    RuntimeException exception = new RuntimeException("Simulates a failing Supplier");

    AtomicBoolean called = new AtomicBoolean(false);
    try {
      underTest.logCeActivity(
        createLogger(logLevel),
        () -> {
          assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isEqualTo(expectedFlag);
          called.compareAndSet(false, true);
          throw exception;
        });
      fail("exception should have been raised");
    } catch (Exception e) {
      assertThat(e).isSameAs(exception);
    } finally {
      assertThat(MDC.get(MDC_CE_ACTIVITY_FLAG)).isNull();
      assertThat(called.get()).isTrue();
    }
  }

}
