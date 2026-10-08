/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.application.es;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.MessageException;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

public class EsDiskSpaceCheckerTest {

  private static final long GB = 1024L * 1024 * 1024;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final FileStore fileStore = mock(FileStore.class);
  private final List<Path> resolvedPaths = new ArrayList<>();
  private final EsDiskSpaceChecker underTest = new EsDiskSpaceChecker(path -> {
    resolvedPaths.add(path);
    return fileStore;
  });

  @Test
  public void check_whenEnoughFreeSpace_shouldNotFail() throws IOException {
    mockDiskSpace(100 * GB, 10 * GB);

    assertThatCode(() -> underTest.check(newEsInstallation())).doesNotThrowAnyException();
  }

  @Test
  public void check_whenLessThan10PercentFree_shouldFail() throws IOException {
    mockDiskSpace(100 * GB, 9 * GB);
    EsInstallation esInstallation = newEsInstallation();

    assertThatThrownBy(() -> underTest.check(esInstallation))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("Not enough free disk space for Elasticsearch data directory")
      .hasMessageContaining("9.0 GB available out of 100.0 GB (9.0%), at least 10.0 GB is required");
  }

  @Test
  public void check_whenFreeSpaceJustBelowThreshold_shouldNotRoundUpToRequiredValues() throws IOException {
    mockDiskSpace(100 * GB, 100 * GB * 996 / 10_000);
    EsInstallation esInstallation = newEsInstallation();

    assertThatThrownBy(() -> underTest.check(esInstallation))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("9.9 GB available out of 100.0 GB (9.9%), at least 10.0 GB is required");
  }

  @Test
  public void check_whenLargeDiskWithMoreThan150GbFree_shouldNotFail() throws IOException {
    mockDiskSpace(10_000 * GB, 150 * GB);

    assertThatCode(() -> underTest.check(newEsInstallation())).doesNotThrowAnyException();
  }

  @Test
  public void check_whenLargeDiskWithLessThan150GbFree_shouldFail() throws IOException {
    mockDiskSpace(10_000 * GB, 149 * GB);
    EsInstallation esInstallation = newEsInstallation();

    assertThatThrownBy(() -> underTest.check(esInstallation))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("at least 150.0 GB is required");
  }

  @Test
  public void check_whenDiskThresholdDisabled_shouldNotFail() throws IOException {
    mockDiskSpace(100 * GB, 0);

    assertThatCode(() -> underTest.check(newEsInstallation().setDiskThresholdEnabled(false))).doesNotThrowAnyException();
    assertThat(resolvedPaths).isEmpty();
  }

  @Test
  public void check_whenDiskSpaceCannotBeRead_shouldNotFail() throws IOException {
    when(fileStore.getTotalSpace()).thenThrow(new IOException("error"));

    assertThatCode(() -> underTest.check(newEsInstallation())).doesNotThrowAnyException();
  }

  @Test
  public void check_whenTotalSpaceUnknown_shouldNotFail() throws IOException {
    mockDiskSpace(0, 0);

    assertThatCode(() -> underTest.check(newEsInstallation())).doesNotThrowAnyException();
  }

  @Test
  public void check_whenDataDirectoryDoesNotExistYet_shouldCheckClosestExistingParent() throws IOException {
    mockDiskSpace(100 * GB, 50 * GB);
    EsInstallation esInstallation = newEsInstallation();
    assertThat(esInstallation.getDataDirectory()).doesNotExist();

    underTest.check(esInstallation);

    assertThat(resolvedPaths).containsExactly(esInstallation.getDataDirectory().getParentFile().toPath().toAbsolutePath());
  }

  private void mockDiskSpace(long totalSpace, long usableSpace) throws IOException {
    when(fileStore.getTotalSpace()).thenReturn(totalSpace);
    when(fileStore.getUsableSpace()).thenReturn(usableSpace);
  }

  private EsInstallation newEsInstallation() throws IOException {
    File dataDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), dataDir.getAbsolutePath());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
    return new EsInstallation(props);
  }
}
