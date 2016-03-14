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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sonar.api.config.Settings;
import org.sonar.ce.log.CeLogAcceptFilter;
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.log.LogFileRef;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;
import org.sonar.ce.queue.CeTask;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.log.CeLogging.MDC_LOG_PATH;

public class CeLoggingTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private LogbackHelper helper = new LogbackHelper();
  private File dataDir;

  @Before
  public void setUp() throws Exception {
    this.dataDir = temp.newFolder();
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
  public void getFile() throws IOException {
    Settings settings = newSettings(dataDir, 10);

    CeLogging underTest = new CeLogging(settings);
    LogFileRef ref = new LogFileRef("TYPE1", "TASK1", "COMPONENT1");

    // file does not exist
    Optional<File> file = underTest.getFile(ref);
    assertThat(file.isPresent()).isFalse();

    File logFile = new File(dataDir, "ce/logs/" + ref.getRelativePath());
    FileUtils.touch(logFile);
    file = underTest.getFile(ref);
    assertThat(file.isPresent()).isTrue();
    assertThat(file.get()).isEqualTo(logFile);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_if_data_dir_is_not_set() {
    new CeLogging(new Settings());
  }

  @Test
  public void initForTask_adds_path_of_ce_log_file_in_MDC() throws IOException {
    CeLogging underTest = new CeLogging(newSettings(dataDir, 5));

    CeTask task = createCeTask("TYPE1", "U1");
    underTest.initForTask(task);
    assertThat(MDC.get(MDC_LOG_PATH)).isNotEmpty().isEqualTo(LogFileRef.from(task).getRelativePath());
  }

  @Test
  public void clearForTask_throws_ISE_if_CE_appender_is_not_configured() throws IOException {
    CeLogging underTest = new CeLogging(newSettings(dataDir, 5));

    CeTask task = createCeTask("TYPE1", "U1");
    underTest.initForTask(task);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Appender with name ce is null or not a SiftingAppender");

    underTest.clearForTask();
  }

  @Test
  public void clearForTask_throws_ISE_if_CE_appender_is_not_a_SiftingAppender() throws IOException {
    Appender<ILoggingEvent> mockCeAppender = mock(Appender.class);
    when(mockCeAppender.getName()).thenReturn("ce");
    helper.getRootContext().getLogger(Logger.ROOT_LOGGER_NAME).addAppender(mockCeAppender);

    CeLogging underTest = new CeLogging(newSettings(dataDir, 5));

    CeTask task = createCeTask("TYPE1", "U1");
    underTest.initForTask(task);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Appender with name ce is null or not a SiftingAppender");

    underTest.clearForTask();
  }

  @Test
  public void clearForTask_clears_MDC() throws IOException {
    setupCeAppender();

    CeLogging underTest = new CeLogging(newSettings(dataDir, 5));

    CeTask task = createCeTask("TYPE1", "U1");
    underTest.initForTask(task);
    assertThat(MDC.get(MDC_LOG_PATH)).isNotEmpty().isEqualTo(LogFileRef.from(task).getRelativePath());

    underTest.clearForTask();
    assertThat(MDC.get(MDC_LOG_PATH)).isNull();
  }

  @Test
  public void cleanForTask_stops_only_appender_for_MDC_value() throws IOException {
    Logger rootLogger = setupCeAppender();

    CeLogging underTest = new CeLogging(newSettings(dataDir, 5));

    // init MDC
    underTest.initForTask(createCeTask("TYPE1", "U1"));
    verifyNoAppender(rootLogger);

    // logging will create and start the appender
    LoggerFactory.getLogger(getClass()).info("some log!");
    verifyAllAppenderStarted(rootLogger, 1);

    // init MDC and create appender for another task
    // (in the same thread, which should not happen, but it's good enough for our test)
    CeTask ceTask = createCeTask("TYPE1", "U2");
    underTest.initForTask(ceTask);
    LoggerFactory.getLogger(getClass()).info("some other log!");
    verifyAllAppenderStarted(rootLogger, 2);

    // stop appender which is currently referenced in MDC
    underTest.clearForTask();

    Appender appender = verifySingleAppenderIsStopped(rootLogger, 2);
    assertThat(appender.getName()).isEqualTo("ce-" + LogFileRef.from(ceTask).getRelativePath());
  }

  @Test
  public void delete_oldest_files_of_same_directory_to_keep_only_max_allowed_files() throws IOException {
    for (int i = 1; i <= 5; i++) {
      File file = new File(dataDir, format("U%d.log", i));
      FileUtils.touch(file);
      // see javadoc: "all platforms support file-modification times to the nearest second,
      // but some provide more precision" --> increment by second, not by millisecond
      file.setLastModified(1_450_000_000_000L + i * 1000);
    }
    assertThat(dataDir.listFiles()).hasSize(5);

    // keep 3 files in each dir
    CeLogging underTest = new CeLogging(newSettings(dataDir, 3));
    underTest.purgeDir(dataDir);

    assertThat(dataDir.listFiles()).hasSize(3);
    assertThat(dataDir.listFiles()).extracting("name")
      .containsOnly("U3.log", "U4.log", "U5.log");
  }

  @Test
  public void do_not_delete_files_if_dir_has_less_files_than_max_allowed() throws IOException {
    FileUtils.touch(new File(dataDir, "U1.log"));

    CeLogging underTest = new CeLogging(newSettings(dataDir, 5));
    underTest.purgeDir(dataDir);

    assertThat(dataDir.listFiles()).extracting("name").containsOnly("U1.log");
  }

  @Test
  public void do_not_keep_any_logs() throws IOException {
    FileUtils.touch(new File(dataDir, "U1.log"));

    CeLogging underTest = new CeLogging(newSettings(dataDir, 0));
    underTest.purgeDir(dataDir);

    assertThat(dataDir.listFiles()).isEmpty();
  }

  @Test
  public void fail_if_max_logs_settings_is_negative() throws IOException {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Property sonar.ce.maxLogsPerTask must be positive. Got: -1");

    Settings settings = newSettings(dataDir, -1);
    CeLogging logging = new CeLogging(settings);
    logging.purgeDir(dataDir);
  }

  @Test
  public void createConfiguration() throws Exception {
    SiftingAppender siftingAppender = CeLogging.createAppenderConfiguration(new LoggerContext(), dataDir);

    // filter on CE logs
    List<Filter<ILoggingEvent>> filters = siftingAppender.getCopyOfAttachedFiltersList();
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0)).isInstanceOf(CeLogAcceptFilter.class);

    assertThat(siftingAppender.getDiscriminator().getKey()).isEqualTo(MDC_LOG_PATH);
    assertThat(siftingAppender.getTimeout().getMilliseconds()).isEqualTo(1000 * 60 * 2);
  }

  private Logger setupCeAppender() {
    Logger rootLogger = helper.getRootContext().getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(CeLogging.createAppenderConfiguration(helper.getRootContext(), dataDir));
    return rootLogger;
  }

  private void verifyNoAppender(Logger rootLogger) {
    Collection<Appender<ILoggingEvent>> allAppenders = getAllAppenders(rootLogger);
    assertThat(allAppenders).isEmpty();
  }

  private void verifyAllAppenderStarted(Logger rootLogger, int expectedSize) {
    Collection<Appender<ILoggingEvent>> allAppenders = getAllAppenders(rootLogger);
    assertThat(allAppenders).hasSize(expectedSize);
    for (Appender<ILoggingEvent> appender : allAppenders) {
      assertThat(appender.isStarted()).isTrue();
    }
  }

  private Appender verifySingleAppenderIsStopped(Logger rootLogger, int expectedSize) {
    Collection<Appender<ILoggingEvent>> allAppenders = getAllAppenders(rootLogger);
    assertThat(allAppenders).hasSize(expectedSize);
    Appender res = null;
    for (Appender<ILoggingEvent> appender : allAppenders) {
      if (!appender.isStarted()) {
        assertThat(res).describedAs("More than one appender found stopped").isNull();
        res = appender;
      }
    }
    assertThat(res).describedAs("There should be one stopped appender").isNotNull();
    return res;
  }

  private Collection<Appender<ILoggingEvent>> getAllAppenders(Logger rootLogger) {
    Appender<ILoggingEvent> ceAppender = rootLogger.getAppender("ce");
    assertThat(ceAppender).isInstanceOf(SiftingAppender.class);
    return ((SiftingAppender) ceAppender).getAppenderTracker().allComponents();
  }

  private static Settings newSettings(File dataDir, int maxLogs) {
    Settings settings = new Settings();
    settings.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    settings.setProperty(CeLogging.MAX_LOGS_PROPERTY, maxLogs);
    return settings;
  }

  private static CeTask createCeTask(String type, String uuid) {
    return new CeTask.Builder().setType(type).setUuid(uuid).build();
  }
}
