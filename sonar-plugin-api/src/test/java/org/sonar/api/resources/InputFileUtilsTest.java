/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.junit.Test;

import java.io.*;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class InputFileUtilsTest {

  @Test
  public void shouldCreateInputFileWithRelativePath() {
    java.io.File basedir = new java.io.File("target/tmp/InputFileUtilsTest");
    
    InputFile inputFile = InputFileUtils.create(basedir, "org/sonar/Foo.java");

    assertThat(inputFile.getFileBaseDir(), is(basedir));
    assertThat(inputFile.getFile(), is(new java.io.File("target/tmp/InputFileUtilsTest/org/sonar/Foo.java")));
    assertThat(inputFile.getRelativePath(), is("org/sonar/Foo.java"));
  }

  @Test
  public void shouldNotAcceptFileWithWrongBasedir() {
    java.io.File basedir1 = new java.io.File("target/tmp/InputFileUtilsTest/basedir1");
    java.io.File basedir2 = new java.io.File("target/tmp/InputFileUtilsTest/basedir2");

    InputFile inputFile = InputFileUtils.create(basedir1, new File(basedir2, "org/sonar/Foo.java"));

    assertThat(inputFile, nullValue());
  }

  @Test
  public void shouldGuessRelativePath() {
    java.io.File basedir = new java.io.File("target/tmp/InputFileUtilsTest");

    java.io.File file = new java.io.File(basedir, "org/sonar/Foo.java");
    InputFile inputFile = InputFileUtils.create(basedir, file);

    assertThat(inputFile.getFileBaseDir(), is(basedir));
    assertThat(inputFile.getFile(), is(file));
    assertThat(inputFile.getRelativePath(), is("org/sonar/Foo.java"));
  }

  @Test
  public void testEqualsAndHashCode() {
    java.io.File basedir = new java.io.File("target/tmp/InputFileUtilsTest");

    InputFile inputFile1 = InputFileUtils.create(basedir, "org/sonar/Foo.java");
    InputFile inputFile2 = InputFileUtils.create(basedir, "org/sonar/Foo.java");

    assertEquals(inputFile1, inputFile1);
    assertEquals(inputFile1, inputFile2);

    assertEquals(inputFile1.hashCode(), inputFile1.hashCode());
  }

  @Test
  public void shouldNotEqualFile() {
    java.io.File basedir = new java.io.File("target/tmp/InputFileUtilsTest");
    File file = new File(basedir, "org/sonar/Foo.java");
    InputFile inputFile = InputFileUtils.create(basedir, file);

    assertThat(inputFile.getFile(), is(file));
    assertThat(inputFile.equals(file), is(false));
  }

  @Test
  public void shouldNotEqualIfBasedirAreDifferents() {
    InputFile inputFile1 = InputFileUtils.create(new File("target/tmp/InputFileUtilsTest"), "org/sonar/Foo.java");
    InputFile inputFile2 = InputFileUtils.create(new File("target/tmp/InputFileUtilsTest/org"), "sonar/Foo.java");
    assertThat(inputFile1.equals(inputFile2), is(false));
  }

  @Test
  public void testToString() {
    File basedir = new File("target/tmp/InputFileUtilsTest");
    InputFile inputFile = InputFileUtils.create(basedir, "org/sonar/Foo.java");
    assertThat(inputFile.toString(), endsWith("InputFileUtilsTest -> org/sonar/Foo.java"));
  }

  @Test
  public void testToFiles() {
    File basedir = new File("target/tmp/InputFileUtilsTest");
    Collection<InputFile> inputFiles = Arrays.asList(
        InputFileUtils.create(basedir, "Foo.java"), InputFileUtils.create(basedir, "Bar.java"));

    Collection<File> files = InputFileUtils.toFiles(inputFiles);

    assertThat(files.size(), is(2));
    Iterator<File> it = files.iterator();
    assertThat(it.next(), is(new File(basedir, "Foo.java")));
    assertThat(it.next(), is(new File(basedir, "Bar.java")));
  }
}
