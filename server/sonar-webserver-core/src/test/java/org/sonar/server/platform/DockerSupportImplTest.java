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
package org.sonar.server.platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.server.util.Paths2;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerSupportImplTest {
  private static final String CGROUP_DIR = "/proc/1/cgroup";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Paths2 paths2 = mock(Paths2.class);
  private DockerSupportImpl underTest = new DockerSupportImpl(paths2);

  @Test
  public void isInDocker_returns_false_if_cgroup_file_does_not_exist() throws IOException {
    Path emptyFile = temporaryFolder.newFile().toPath();
    Files.delete(emptyFile);
    when(paths2.get(CGROUP_DIR)).thenReturn(emptyFile);

    assertThat(underTest.isRunningInDocker()).isFalse();
  }

  @Test
  public void isInDocker_returns_false_if_cgroup_file_is_empty() throws IOException {
    Path emptyFile = temporaryFolder.newFile().toPath();
    when(paths2.get(CGROUP_DIR)).thenReturn(emptyFile);

    assertThat(underTest.isRunningInDocker()).isFalse();
  }

  @Test
  public void isInDocker_returns_false_if_cgroup_dir_contains_no_file_with_slash_docker_string() throws IOException {
    Path cgroupFile = temporaryFolder.newFile().toPath();
    String content = "11:name=systemd:/" + lineSeparator() +
      "10:hugetlb:/" + lineSeparator() +
      "9:perf_event:/" + lineSeparator() +
      "8:blkio:/" + lineSeparator() +
      "7:freezer:/" + lineSeparator() +
      "6:devices:/" + lineSeparator() +
      "5:memory:/" + lineSeparator() +
      "4:cpuacct:/" + lineSeparator() +
      "3:cpu:/" + lineSeparator() +
      "2:cpuset:/";
    FileUtils.write(cgroupFile.toFile(), content, StandardCharsets.UTF_8);
    when(paths2.get(CGROUP_DIR)).thenReturn(cgroupFile);

    assertThat(underTest.isRunningInDocker()).isFalse();
  }

  @Test
  public void isInDocker_returns_true_if_cgroup_dir_contains_file_with_slash_docker_string() throws IOException {
    Path cgroupFile = temporaryFolder.newFile().toPath();
    String content = "11:name=systemd:/" + lineSeparator() +
      "10:hugetlb:/" + lineSeparator() +
      "9:perf_event:/" + lineSeparator() +
      "8:blkio:/" + lineSeparator() +
      "7:freezer:/" + lineSeparator() +
      "6:devices:/docker/3601745b3bd54d9780436faa5f0e4f72bb46231663bb99a6bb892764917832c2" + lineSeparator() +
      "5:memory:/" + lineSeparator() +
      "4:cpuacct:/" + lineSeparator() +
      "3:cpu:/docker/3601745b3bd54d9780436faa5f0e4f72bb46231663bb99a6bb892764917832c2" + lineSeparator() +
      "2:cpuset:/";
    FileUtils.write(cgroupFile.toFile(), content, StandardCharsets.UTF_8);
    when(paths2.get(CGROUP_DIR)).thenReturn(cgroupFile);

    assertThat(underTest.isRunningInDocker()).isTrue();
  }

}
