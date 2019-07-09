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
package org.sonar.api.batch.fs.internal.fs;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputDir;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultInputDirTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test() throws Exception {
    File baseDir = temp.newFolder();
    DefaultInputDir inputDir = new DefaultInputDir("ABCDE", "src")
      .setModuleBaseDir(baseDir.toPath());

    assertThat(inputDir.key()).isEqualTo("ABCDE:src");
    assertThat(inputDir.file().getAbsolutePath()).isEqualTo(new File(baseDir, "src").getAbsolutePath());
    assertThat(inputDir.relativePath()).isEqualTo("src");
    assertThat(new File(inputDir.relativePath())).isRelative();
    assertThat(inputDir.absolutePath()).endsWith("src");
    assertThat(new File(inputDir.absolutePath())).isAbsolute();
  }

  @Test
  public void testEqualsAndHashCode() throws Exception {
    DefaultInputDir inputDir1 = new DefaultInputDir("ABCDE", "src");

    DefaultInputDir inputDir2 = new DefaultInputDir("ABCDE", "src");

    assertThat(inputDir1.equals(inputDir1)).isTrue();
    assertThat(inputDir1.equals(inputDir2)).isTrue();
    assertThat(inputDir1.equals("foo")).isFalse();

    assertThat(inputDir1.hashCode()).isEqualTo(63545559);

    assertThat(inputDir1.toString()).contains("[moduleKey=ABCDE, relative=src, basedir=null");

  }

}
