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
package org.sonar.xoo.scm;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.xoo.scm.XooIgnoreCommand.IGNORE_FILE_EXTENSION;

public class XooIgnoreCommandTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
  }

  @Test
  public void testBlame() throws IOException {
    File source = newFile("foo.xoo", false);
    File source1 = newFile("foo2.xoo", true);

    XooIgnoreCommand ignoreCommand = new XooIgnoreCommand();
    ignoreCommand.init(baseDir.toPath());

    assertThat(ignoreCommand.isIgnored(source.toPath())).isFalse();
    assertThat(ignoreCommand.isIgnored(source1.toPath())).isTrue();
  }

  private File newFile(String name, boolean isIgnored) throws IOException {
    File source = new File(baseDir, name);
    source.createNewFile();
    if (isIgnored) {
      File ignoredMetaFile = new File(baseDir, name + IGNORE_FILE_EXTENSION);
      ignoredMetaFile.createNewFile();
    }

    return source;
  }
}
