package org.sonar.api.batch.fs.internal;

/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DeprecatedDefaultInputFileTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test() throws Exception {
    DeprecatedDefaultInputFile inputFile = (DeprecatedDefaultInputFile) new DeprecatedDefaultInputFile("ABCDE", "src/Foo.php")
      .setModuleBaseDir(temp.newFolder().toPath())
      .setLines(42)
      .setLanguage("php")
      .setStatus(InputFile.Status.ADDED)
      .setType(InputFile.Type.TEST);

    assertThat(inputFile.relativePath()).isEqualTo("src/Foo.php");
    // deprecated method is different -> path relative to source dir
    assertThat(inputFile.getRelativePath()).isEqualTo("src/Foo.php");
    assertThat(new File(inputFile.relativePath())).isRelative();
    assertThat(inputFile.absolutePath()).endsWith("Foo.php");
    assertThat(new File(inputFile.absolutePath())).isAbsolute();
    assertThat(inputFile.language()).isEqualTo("php");
    assertThat(inputFile.status()).isEqualTo(InputFile.Status.ADDED);
    assertThat(inputFile.type()).isEqualTo(InputFile.Type.TEST);
    assertThat(inputFile.lines()).isEqualTo(42);
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    DefaultInputFile f1 = new DefaultInputFile("ABCDE", "src/Foo.php");
    DefaultInputFile f1a = new DefaultInputFile("ABCDE", "src/Foo.php");
    DefaultInputFile f2 = new DefaultInputFile("ABCDE", "src/Bar.php");

    assertThat(f1).isEqualTo(f1);
    assertThat(f1).isEqualTo(f1a);
    assertThat(f1).isNotEqualTo(f2);
    assertThat(f1.equals("foo")).isFalse();
    assertThat(f1.equals(null)).isFalse();

    assertThat(f1.hashCode()).isEqualTo(f1.hashCode());
    assertThat(f1.hashCode()).isEqualTo(f1a.hashCode());
  }

  @Test
  public void test_toString() throws Exception {
    DefaultInputFile file = new DefaultInputFile("ABCDE", "src/Foo.php");
    assertThat(file.toString()).isEqualTo("[moduleKey=ABCDE, relative=src/Foo.php, basedir=null]");
  }
}
