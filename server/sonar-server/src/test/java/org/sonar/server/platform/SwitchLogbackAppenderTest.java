package org.sonar.server.platform;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.Test;
import org.sonar.server.computation.AnalysisReportTaskLauncher;

import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;

public class SwitchLogbackAppenderTest {

  LoggerContext loggerContext = new LoggerContext();
  Logger logger = loggerContext.getLogger(this.getClass().getName());
  ListAppender<ILoggingEvent> console;
  ListAppender<ILoggingEvent> analyisReport;

  protected void configure(URL file) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(loggerContext);
    jc.doConfigure(file);

    Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    SwitchLogbackAppender switchAppender = (SwitchLogbackAppender) root.getAppender("SWITCH");
    console = (ListAppender<ILoggingEvent>) switchAppender.getAppender("CONSOLE");
    analyisReport = (ListAppender<ILoggingEvent>) switchAppender.getAppender("ANALYSIS_REPORTS");
  }

  /**
   * Standard log goes to console only
   */
  @Test
  public void standard_log() throws JoranException {
    configure(getClass().getResource("SwitchLogbackAppenderTest/valid-switch.xml"));

    logger.info("hello");

    assertThat(console.list).hasSize(1);
    assertThat(console.list.get(0).getMessage()).isEqualTo("hello");
    assertThat(analyisReport.list).isEmpty();
  }

  /**
   * Compute service log goes to dedicated appender. Warnings and errors are logged in both appenders.
   */
  @Test
  public void compute_service_log_goes_to_dedicated_appender() throws JoranException {
    configure(getClass().getResource("SwitchLogbackAppenderTest/valid-switch.xml"));

    String initialThreadName = Thread.currentThread().getName();
    Thread.currentThread().setName(AnalysisReportTaskLauncher.ANALYSIS_REPORT_THREAD_NAME_PREFIX + "test");
    try {
      logger.info("hello");
      assertThat(analyisReport.list).hasSize(1);
      assertThat(analyisReport.list.get(0).getMessage()).isEqualTo("hello");
      assertThat(console.list).isEmpty();

      logger.warn("a warning");
      assertThat(analyisReport.list).hasSize(2);
      assertThat(analyisReport.list.get(1).getMessage()).isEqualTo("a warning");
      assertThat(console.list).hasSize(1);
      assertThat(console.list.get(0).getMessage()).isEqualTo("a warning");

      logger.warn("an error");
      assertThat(analyisReport.list).hasSize(3);
      assertThat(analyisReport.list.get(2).getMessage()).isEqualTo("an error");
      assertThat(console.list).hasSize(2);
      assertThat(console.list.get(1).getMessage()).isEqualTo("an error");

    } finally {
      Thread.currentThread().setName(initialThreadName);
    }
  }

  @Test
  public void fail_if_bad_configuration() throws JoranException {
    boolean foundError = false;
    configure(getClass().getResource("SwitchLogbackAppenderTest/invalid-switch.xml"));
    StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    for (Status status : loggerContext.getStatusManager().getCopyOfStatusList()) {
      Throwable error = status.getThrowable();
      if (error != null) {
        assertThat(error).hasMessage("Invalid appender name: UNKNOWN");
        foundError = true;
      }
    }
    assertThat(foundError).isTrue();
  }
}
