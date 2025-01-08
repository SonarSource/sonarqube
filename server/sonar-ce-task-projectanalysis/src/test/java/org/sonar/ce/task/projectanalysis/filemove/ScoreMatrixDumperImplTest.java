/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.filemove.ScoreMatrix.ScoreFile;
import org.sonar.server.platform.ServerFileSystem;

import static org.apache.commons.lang3.RandomStringUtils.secure;
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

  private ServerFileSystem serverFileSystem = mock(ServerFileSystem.class);
  private ScoreMatrixDumper underTest = new ScoreMatrixDumperImpl(configuration, ceTask, serverFileSystem);
  private Path tempDir;

  @Before
  public void before() throws IOException {
    Path tempFile = Files.createTempFile("a", "b");
    Files.delete(tempFile);
    tempDir = tempFile.getParent();
    when(serverFileSystem.getTempDir()).thenReturn(tempDir.toFile());
  }

  @After
  public void cleanUp() {
    try {
      Files.list(tempDir.toAbsolutePath()).filter(p -> p.getFileName().toString().contains("score-matrix-")).forEach((p) -> {
        try {
          Files.deleteIfExists(p);
        } catch (Exception e) {
          System.out.println("Could not delete file. Details: " + e.getMessage());
        }
      });
    } catch (Exception e) {
      System.out.println("Cleaning up temp directory failed. Details: " + e.getMessage());
    }
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
    String taskUuid = secure().nextAlphabetic(6);
    when(ceTask.getUuid()).thenReturn(taskUuid);

    underTest.dumpAsCsv(A_SCORE_MATRIX);

    assertThat(listDumpFilesForTaskUuid(taskUuid)).isEmpty();
  }

  @Test
  @UseDataProvider("notTruePropertyValues")
  public void dumpAsCsv_has_no_effect_if_property_is_not_true(String value) throws IOException {
    String taskUuid = secure().nextAlphabetic(6);
    when(ceTask.getUuid()).thenReturn(taskUuid);
    settings.setProperty("sonar.filemove.dumpCsv", value);

    underTest.dumpAsCsv(A_SCORE_MATRIX);

    assertThat(listDumpFilesForTaskUuid(taskUuid)).isEmpty();
  }

  @DataProvider
  public static Object[][] notTruePropertyValues() {
    return new Object[][] {
      {secure().nextAlphabetic(6)},
      {"false"},
    };
  }

  private Collection<File> listDumpFilesForTaskUuid(String taskUuid) {
    Collection<File> dumpFiles = new ArrayList<>();
    File dir = tempDir.toFile();
    File[] files = dir.listFiles();
    if (!dir.exists() || files == null) {
      throw new IllegalStateException("Temp directory does not exist");
    }
    for (File file : files) {
      if (file.exists()) {
        String name = file.getName();
        if (name.startsWith("score-matrix-" + taskUuid) && name.endsWith(".csv")) {
          dumpFiles.add(file);
        }
      }
    }

    return dumpFiles;
  }
}
