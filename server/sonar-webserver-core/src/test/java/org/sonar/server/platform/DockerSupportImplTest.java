/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
  private static final String PODMAN_FILE_PATH = "/run/.containerenv";

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

  @Test
  public void isInDocker_returns_true_if_cgroup_dir_contains_file_with_kubepods_string() throws IOException {
    Path cgroupFile = temporaryFolder.newFile().toPath();
    String content = "11:blkio:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "10:cpuset:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "9:net_cls,net_prio:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "8:pids:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "7:perf_event:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "6:freezer:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "5:hugetlb:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "4:memory:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "3:devices:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "2:cpu,cpuacct:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366" + lineSeparator() +
      "1:name=systemd:/kubepods/burstable/pod8e9a7fc0-4e11-4497-a424-19b9713eff0e/8953402928cc7fc95c7dc7bdb75b194139fe29e8fa196d7f90924deb29164366";
    FileUtils.write(cgroupFile.toFile(), content, StandardCharsets.UTF_8);
    when(paths2.get(CGROUP_DIR)).thenReturn(cgroupFile);

    assertThat(underTest.isRunningInDocker()).isTrue();
  }

  @Test
  public void isInDocker_returns_true_if_cgroup_dir_contains_file_with_containerd_string() throws IOException {
    Path cgroupFile = temporaryFolder.newFile().toPath();
    String content = "12:blkio:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "11:perf_event:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "10:hugetlb:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "9:pids:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "8:rdma:/" + lineSeparator() +
      "7:memory:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "6:cpuset:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "5:net_cls,net_prio:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "4:freezer:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "3:cpu,cpuacct:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "2:devices:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "1:name=systemd:/default/846fe494c3021f068c9156ca6eb8a91038389b7e2a2b1ae9b050b33c3a5c9298" + lineSeparator() +
      "0::/system.slice/containerd.service";
    FileUtils.write(cgroupFile.toFile(), content, StandardCharsets.UTF_8);
    when(paths2.get(CGROUP_DIR)).thenReturn(cgroupFile);

    assertThat(underTest.isRunningInDocker()).isTrue();
  }

  @Test
  public void isInDocker_returns_true_if_podman_file_exists() throws IOException {
    when(paths2.exists(PODMAN_FILE_PATH)).thenReturn(true);
    assertThat(underTest.isRunningInDocker()).isTrue();
  }

  @Test
  public void isInDocker_returns_false_if_podman_file_exists() throws IOException {
    when(paths2.exists(PODMAN_FILE_PATH)).thenReturn(false);
    Path emptyFile = temporaryFolder.newFile().toPath();
    when(paths2.get(CGROUP_DIR)).thenReturn(emptyFile);
    assertThat(underTest.isRunningInDocker()).isFalse();
  }

}
