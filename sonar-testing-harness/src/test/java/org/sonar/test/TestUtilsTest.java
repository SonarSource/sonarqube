/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.sonar.test.TestUtils.assertExists;
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
    TestUtils.assertExists(file);

    file = getResource("/org/sonar/test/TestUtilsTest/getResource/foo.txt");
    TestUtils.assertExists(file);

    file = getResource(getClass(), "getResource/foo.txt");
    TestUtils.assertExists(file);
  }

  @Test
  public void testResourceNotFound() {
    File file = getResource("org/sonar/test/TestUtilsTest/unknown.txt");
    assertNull(file);
  }

  @Test
  public void testTempDir() throws Exception {
    File dir = getTestTempDir(getClass(), "testTempDir");
    assertExists(dir);
    assertThat(dir.isDirectory(), is(true));
    assertThat(dir.listFiles().length, is(0));

    FileUtils.writeStringToFile(new File(dir, "bar.txt"), "some text");
    assertThat(dir.listFiles().length, is(1));

    // the directory is cleaned
    dir = getTestTempDir(getClass(), "testTempDir");
    TestUtils.assertExists(dir);
    assertThat(dir.isDirectory(), is(true));
    assertThat(dir.listFiles().length, is(0));
  }

  @Test
  public void testAssertSimilarXml() throws Exception {
    assertSimilarXml("<foo></foo>", "<foo />");

    // order of attributes
    assertSimilarXml("<foo><bar id='1' key='one' /></foo>", "<foo><bar key='one' id='1' /></foo>");

    // whitespaces are ignored
    assertSimilarXml("<foo>     <bar />   </foo>", "<foo><bar/></foo>");

    // attribute values are checked
    assertFalse(isSimilarXml("<foo id='1' />", "<foo id='2'/>").similar());

    // different xml
    assertFalse(isSimilarXml("<foo id='1' />", "<foo id='2'/>").similar());

    // order of nodes is important
    assertFalse(isSimilarXml("<foo><bar id='1' /><bar id='2' /></foo>", "<foo><bar id='2' /><bar id='1' /></foo>").similar());
  }
}
