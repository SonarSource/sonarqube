package org.sonar.server.util;

import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProgressLoggerTest {

  @Test(timeout = 1000L)
  public void log_at_fixed_intervals() throws Exception {
    Logger logger = mock(Logger.class);
    AtomicLong counter = new AtomicLong(42L);
    ProgressLogger progress = new ProgressLogger("ProgressLoggerTest", counter, logger);
    progress.setPeriodMs(1L);
    progress.start();
    Thread.sleep(20L);
    progress.stop();
    verify(logger, atLeast(1)).info("42 rows processed");

    // ability to manual log, generally final status
    counter.incrementAndGet();
    progress.log();
    verify(logger).info("43 rows processed");
  }

  @Test
  public void create() throws Exception {
    ProgressLogger progress = ProgressLogger.create(getClass(), new AtomicLong());

    // default values
    assertThat(progress.getPeriodMs()).isEqualTo(60000L);
    assertThat(progress.getPluralLabel()).isEqualTo("rows");

    // override values
    progress.setPeriodMs(10L);
    progress.setPluralLabel("issues");
    assertThat(progress.getPeriodMs()).isEqualTo(10L);
    assertThat(progress.getPluralLabel()).isEqualTo("issues");

  }
}
