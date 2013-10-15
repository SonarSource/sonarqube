/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.bootstrap;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class BatchTempUtilsTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createTempFolder() throws Exception {
    File workingDir = temp.newFolder();
    BatchTempUtils tempUtils = new BatchTempUtils(new BootstrapSettings(
      new BootstrapProperties(ImmutableMap.of(CoreProperties.WORKING_DIRECTORY, workingDir.getAbsolutePath()))));
    tempUtils.createTempDirectory();
    tempUtils.createTempFile();
    assertThat(new File(workingDir, "tmp")).exists();
    assertThat(new File(workingDir, "tmp").list()).hasSize(2);

    tempUtils.stop();
    assertThat(new File(workingDir, "tmp")).doesNotExist();
  }
}
