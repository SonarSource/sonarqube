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
package org.sonar.plugins.core.utils;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.SonarException;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class HashBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_compute_hash() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foobar");
    assertThat(new HashBuilder().computeHash(tempFile)).isEqualTo("3858f62230ac3c915f300c664312c63f");
  }

  @Test(expected = SonarException.class)
  public void should_throw_on_not_existing_file() throws Exception {
    File tempFolder = temp.newFolder();
    new HashBuilder().computeHash(new File(tempFolder, "unknowFile.txt"));
  }
}
