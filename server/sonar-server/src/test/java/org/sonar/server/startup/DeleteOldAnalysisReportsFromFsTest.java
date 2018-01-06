/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.startup;

import java.io.File;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.platform.ServerFileSystem;

import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteOldAnalysisReportsFromFsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private ServerUpgradeStatus status = mock(ServerUpgradeStatus.class);
  private ServerFileSystem fs = mock(ServerFileSystem.class);
  private DeleteOldAnalysisReportsFromFs underTest = new DeleteOldAnalysisReportsFromFs(status, fs);
  private File dataDir = null;
  private File reportsDir = null;

  @Before
  public void setUp() throws Exception {
    dataDir = temp.newFolder();
    reportsDir = new File(dataDir, "ce/reports");
    touch(new File(reportsDir, "report1.zip"));
    when(fs.getDataDir()).thenReturn(dataDir);
  }

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void do_nothing_on_regular_startups() {
    when(status.isUpgraded()).thenReturn(false);

    underTest.start();

    assertThat(reportsDir).exists().isDirectory();
    assertThat(dataDir).exists().isDirectory();
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
  }

  @Test
  public void delete_reports_directory_if_upgrade() {
    when(status.isUpgraded()).thenReturn(true);

    underTest.start();

    assertThat(reportsDir).doesNotExist();
    assertThat(dataDir).exists().isDirectory();
    assertThat(logTester.logs(LoggerLevel.INFO)).have(new Condition<>(s ->  s.contains("Delete unused directory of analysis reports"), ""));
  }
}
