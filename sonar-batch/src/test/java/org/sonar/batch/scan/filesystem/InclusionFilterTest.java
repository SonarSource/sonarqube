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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class InclusionFilterTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void accept() throws Exception {
    InclusionFilter sourceRelativeFilter = new InclusionFilter("**/*Foo.java");
    InclusionFilter absoluteFilter = new InclusionFilter("file:**/src/main/**Foo.java");

    File file = new File(temp.newFolder(), "src/main/java/org/MyFoo.java");
    InputFile inputFile = DefaultInputFile.create(file, "src/main/java/org/MyFoo.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "org/MyFoo.java"
    ));

    assertThat(sourceRelativeFilter.accept(inputFile)).isTrue();
    assertThat(absoluteFilter.accept(inputFile)).isTrue();

    file = new File(temp.newFolder(), "src/main/java/org/Other.java");
    inputFile = DefaultInputFile.create(file, "src/main/java/org/Other.java", ImmutableMap.of(
      InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "org/Other.java"
    ));
    assertThat(sourceRelativeFilter.accept(inputFile)).isFalse();
    assertThat(absoluteFilter.accept(inputFile)).isFalse();
  }

  @Test
  public void test_toString() {
    InclusionFilter filter = new InclusionFilter("**/*Foo.java");
    assertThat(filter.toString()).isEqualTo("Includes: **/*Foo.java");
  }
}
