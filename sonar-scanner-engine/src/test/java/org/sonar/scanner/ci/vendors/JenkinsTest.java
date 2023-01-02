/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.ci.vendors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ZipUtils;
import org.sonar.scanner.ci.CiVendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JenkinsTest {
  private System2 system = mock(System2.class);
  private DefaultInputProject project = mock(DefaultInputProject.class);
  private CiVendor underTest = new Jenkins(system, project);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("Jenkins");
  }

  @Test
  public void isDetected() {
    setEnvVariable("JENKINS_URL", "http://foo");
    setEnvVariable("EXECUTOR_NUMBER", "12");
    assertThat(underTest.isDetected()).isTrue();

    setEnvVariable("JENKINS_URL", null);
    setEnvVariable("EXECUTOR_NUMBER", "12");
    assertThat(underTest.isDetected()).isFalse();

    setEnvVariable("JENKINS_URL", "http://foo");
    setEnvVariable("EXECUTOR_NUMBER", null);
    assertThat(underTest.isDetected()).isFalse();
  }

  @Test
  public void loadConfiguration_with_deprecated_pull_request_plugin() {
    setEnvVariable("ghprbActualCommit", "abd12fc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abd12fc");
  }

  @Test
  public void loadConfiguration_of_git_repo() {
    setEnvVariable("GIT_COMMIT", "abd12fc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abd12fc");
  }

  @Test
  public void loadConfiguration_of_git_repo_with_branch_plugin() throws IOException {
    // prepare fake git clone
    Path baseDir = temp.newFolder().toPath();
    File unzip = ZipUtils.unzip(this.getClass().getResourceAsStream("gitrepo.zip"), baseDir.toFile());
    when(project.getBaseDir()).thenReturn(unzip.toPath().resolve("gitrepo"));

    setEnvVariable("CHANGE_ID", "3");
    setEnvVariable("GIT_BRANCH", "PR-3");
    // this will be ignored
    setEnvVariable("GIT_COMMIT", "abd12fc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("e6013986eff4f0ce0a85f5d070070e7fdabead48");
  }

  @Test
  public void loadConfiguration_of_git_repo_with_branch_plugin_without_git_repo() throws IOException {
    // prepare fake git clone
    Path baseDir = temp.newFolder().toPath();
    when(project.getBaseDir()).thenReturn(baseDir);

    setEnvVariable("CHANGE_ID", "3");
    setEnvVariable("GIT_BRANCH", "PR-3");
    setEnvVariable("GIT_COMMIT", "abc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abc");
  }

  @Test
  public void loadConfiguration_of_svn_repo() {
    setEnvVariable("SVN_COMMIT", "abd12fc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abd12fc");
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }

}
