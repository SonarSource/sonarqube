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
package org.sonar.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.test.TestUtils.assertSimilarXml;
import static org.sonar.test.TestUtils.getResource;
import static org.sonar.test.TestUtils.getTestTempDir;
import static org.sonar.test.TestUtils.isSimilarXml;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TestUtilsTest {

  @Test
  public void testResource() {
    File file = getResource("org/sonar/test/TestUtilsTest/getResource/foo.txt");
    assertThat(file).exists();

    file = getResource("/org/sonar/test/TestUtilsTest/getResource/foo.txt");
    assertThat(file).exists();

    file = getResource(getClass(), "getResource/foo.txt");
    assertThat(file).exists();
  }

  @Test
  public void testResourceNotFound() {
    File file = getResource("org/sonar/test/TestUtilsTest/unknown.txt");
    assertThat(file).isNull();
  }

  @Test
  public void testTempDir() throws Exception {
    File dir = getTestTempDir(getClass(), "testTempDir");
    assertThat(dir).exists().isDirectory();
    assertThat(dir.listFiles()).isEmpty();

    FileUtils.writeStringToFile(new File(dir, "bar.txt"), "some text");
    assertThat(dir.listFiles()).hasSize(1);

    // the directory is cleaned
    dir = getTestTempDir(getClass(), "testTempDir");
    assertThat(dir).exists().isDirectory();
    assertThat(dir.listFiles()).isEmpty();
  }

  @Test
  public void testAssertSimilarXml() throws Exception {
    assertSimilarXml("<foo></foo>", "<foo />");

    // order of attributes
    assertSimilarXml("<foo><bar id='1' key='one' /></foo>", "<foo><bar key='one' id='1' /></foo>");

    // whitespaces are ignored
    assertSimilarXml("<foo>     <bar />   </foo>", "<foo><bar/></foo>");

    // attribute values are checked
    assertThat(isSimilarXml("<foo id='1' />", "<foo id='2'/>").similar()).isFalse();

    // different xml
    assertThat(isSimilarXml("<foo id='1' />", "<foo id='2'/>").similar()).isFalse();

    // order of nodes is important
    assertThat(isSimilarXml("<foo><bar id='1' /><bar id='2' /></foo>", "<foo><bar id='2' /><bar id='1' /></foo>").similar()).isFalse();
  }
}
