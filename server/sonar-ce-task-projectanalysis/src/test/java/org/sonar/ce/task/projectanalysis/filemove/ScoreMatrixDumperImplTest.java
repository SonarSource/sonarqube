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
package org.sonar.ce.task.projectanalysis.filemove;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.filemove.ScoreMatrix.ScoreFile;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ScoreMatrixDumperImplTest {
  private static final ScoreMatrix A_SCORE_MATRIX = new ScoreMatrix(
    new ScoreFile[] {new ScoreFile("A", 12), new ScoreFile("B", 8)},
    new ScoreFile[] {new ScoreFile("1", 7)},
    new int[][] {{10}, {2}},
    10);
  private MapSettings settings = new MapSettings();
  private Configuration configuration = settings.asConfig();
  private CeTask ceTask = mock(CeTask.class);
  private ScoreMatrixDumper underTest = new ScoreMatrixDumperImpl(configuration, ceTask);
  private static Path tempDir;

  @BeforeClass
  public static void lookupTempDir() throws IOException {
    Path tempFile = Files.createTempFile("a", "b");
    Files.delete(tempFile);
    tempDir = tempFile.getParent();
  }

  @Before
  public void setUp() throws Exception {
    FileUtils.listFiles(tempDir.toFile(), new AbstractFileFilter() {
      @Override
      public boolean accept(File file) {
        if (file.getName().contains("score-matrix-")) {
          file.delete();
        }
        return false;
      }
    }, null);
  }

  @Test
  public void dumpAsCsv_creates_csv_dump_of_score_matrix_if_property_is_true() throws IOException {
    String taskUuid = "acme";
    when(ceTask.getUuid()).thenReturn(taskUuid);
    settings.setProperty("sonar.filemove.dumpCsv", "true");

    underTest.dumpAsCsv(A_SCORE_MATRIX);

    Collection<File> files = listDumpFilesForTaskUuid(taskUuid);
    assertThat(files).hasSize(1);
    assertThat(files.iterator().next()).hasContent(A_SCORE_MATRIX.toCsv(';'));
  }

  @Test
  public void dumpAsCsv_has_no_effect_if_configuration_is_empty() throws IOException {
    String taskUuid = randomAlphabetic(6);
    when(ceTask.getUuid()).thenReturn(taskUuid);

    underTest.dumpAsCsv(A_SCORE_MATRIX);

    assertThat(listDumpFilesForTaskUuid(taskUuid)).isEmpty();
  }

  @Test
  @UseDataProvider("notTruePropertyValues")
  public void dumpAsCsv_has_no_effect_if_property_is_not_true(String value) throws IOException {
    String taskUuid = randomAlphabetic(6);
    when(ceTask.getUuid()).thenReturn(taskUuid);
    settings.setProperty("sonar.filemove.dumpCsv", value);

    underTest.dumpAsCsv(A_SCORE_MATRIX);

    assertThat(listDumpFilesForTaskUuid(taskUuid)).isEmpty();
  }

  @DataProvider
  public static Object[][] notTruePropertyValues() {
    return new Object[][] {
      {randomAlphabetic(6)},
      {"false"},
    };
  }

  private static Collection<File> listDumpFilesForTaskUuid(String taskUuid) throws IOException {
    return FileUtils.listFiles(tempDir.toFile(), new AbstractFileFilter() {
      @Override
      public boolean accept(File file) {
        String name = file.getName();
        return name.startsWith("score-matrix-" + taskUuid) && name.endsWith(".csv");
      }
    }, null);
  }
}
