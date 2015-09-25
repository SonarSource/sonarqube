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
package org.sonar.server.computation.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.MDC;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;
import org.sonar.server.computation.CeTask;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CeLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void getFile() throws IOException {
    File dataDir = temp.newFolder();
    Settings settings = new Settings();
    settings.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());

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
  public void use_MDC_to_store_path_to_in_progress_task_logs() throws IOException {
    CeLogging underTest = new CeLogging(temp.newFolder());

    CeTask task = new CeTask.Builder().setType("TYPE1").setUuid("U1").build();
    underTest.initForTask(task);
    assertThat(MDC.get(CeLogging.MDC_LOG_PATH)).isNotEmpty().isEqualTo(LogFileRef.from(task).getRelativePath());
    underTest.clearForTask();
    assertThat(MDC.get(CeLogging.MDC_LOG_PATH)).isNull();
  }

  @Test
  public void delete_oldest_files_of_same_directory_to_keep_only_10_files() throws IOException {
    File dir = temp.newFolder();
    for (int i = 1; i <= 14; i++) {
      File file = new File(dir, format("U%d.log", i));
      FileUtils.touch(file);
      // see javadoc: "all platforms support file-modification times to the nearest second,
      // but some provide more precision" --> increment by second, not by millisecond
      file.setLastModified(1_450_000_000_000L + i * 1000);
    }
    assertThat(dir.listFiles()).hasSize(14);

    CeLogging underTest = new CeLogging(dir);
    underTest.purgeDir(dir);

    assertThat(dir.listFiles()).hasSize(10);
    assertThat(dir.listFiles()).extracting("name")
      .containsOnly("U5.log", "U6.log", "U7.log", "U8.log", "U9.log", "U10.log", "U11.log", "U12.log", "U13.log", "U14.log");
  }

  @Test
  public void do_not_delete_files_if_dir_has_less_than_10_files() throws IOException {
    File dir = temp.newFolder();
    FileUtils.touch(new File(dir, "U1.log"));

    CeLogging underTest = new CeLogging(dir);
    underTest.purgeDir(dir);

    assertThat(dir.listFiles()).extracting("name").containsOnly("U1.log");
  }

  @Test
  public void createConfiguration() throws Exception {
    File logsDir = temp.newFolder();
    SiftingAppender siftingAppender = CeLogging.createAppenderConfiguration(new LoggerContext(), logsDir);

    // filter on CE logs
    List<Filter<ILoggingEvent>> filters = siftingAppender.getCopyOfAttachedFiltersList();
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0)).isInstanceOf(CeLogAcceptFilter.class);

    assertThat(siftingAppender.getDiscriminator().getKey()).isEqualTo(CeLogging.MDC_LOG_PATH);
  }

}
