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

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.MDC;
import org.sonar.api.config.Settings;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.process.ProcessProperties;
import org.sonar.server.computation.CeTask;

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
    LogFileRef ref = new LogFileRef("TASK1", "COMPONENT1");

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
    File dataDir = temp.newFolder();
    Settings settings = new Settings();
    settings.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());

    CeLogging underTest = new CeLogging(settings);

    underTest.initTask(new CeTask.Builder().setType(CeTaskTypes.REPORT).setUuid("U1").build());
    assertThat(MDC.get(CeFileAppenderFactory.MDC_LOG_PATH)).isEqualTo("U1.log");
    underTest.clearTask();
    assertThat(MDC.get(CeFileAppenderFactory.MDC_LOG_PATH)).isNull();
  }
}
