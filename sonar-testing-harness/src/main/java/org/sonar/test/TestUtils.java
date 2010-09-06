/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.sonar.api.utils.SonarException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Utilities for unit tests
 * 
 * @since 2.2
 */
public class TestUtils {

  /**
   * Search for a test resource in the classpath. For example getResource("org/sonar/MyClass/foo.txt");
   * 
   * @param path the starting slash is optional
   * @return the resource. Null if resource not found
   */
  public static File getResource(String path) {
    String resourcePath = path;
    if ( !resourcePath.startsWith("/")) {
      resourcePath = "/" + resourcePath;
    }
    URL url = TestUtils.class.getResource(resourcePath);
    if (url != null) {
      return FileUtils.toFile(url);
    }
    return null;
  }

  public static String getResourceContent(String path) {
    File file = getResource(path);
    if (file != null) {
      try {
        return FileUtils.readFileToString(file, CharEncoding.UTF_8);

      } catch (IOException e) {
        throw new SonarException("Can not load the resource: " + path, e);
      }
    }
    return null;
  }

  /**
   * Search for a resource in the classpath. For example calling the method getResource(getClass(), "myTestName/foo.txt") from
   * the class org.sonar.Foo loads the file $basedir/src/test/resources/org/sonar/Foo/myTestName/foo.txt
   * 
   * @return the resource. Null if resource not found
   */
  public static File getResource(Class baseClass, String path) {
    String resourcePath = StringUtils.replaceChars(baseClass.getCanonicalName(), '.', '/');
    if ( !path.startsWith("/")) {
      resourcePath += "/";
    }
    resourcePath += path;
    return getResource(resourcePath);
  }

  /**
   * Shortcut for getTestTempDir(baseClass, testName, true) : cleans the unit test directory
   */
  public static File getTestTempDir(Class baseClass, String testName) {
    return getTestTempDir(baseClass, testName, true);
  }

  /**
   * Create a temporary directory for unit tests.
   * @param baseClass the unit test class
   * @param testName the test name
   * @param clean remove all the sub-directories and files ?
   */
  public static File getTestTempDir(Class baseClass, String testName, boolean clean) {
    File dir = new File("target/test-tmp/" + baseClass.getCanonicalName() + "/" + testName);
    if (clean && dir.exists()) {
      try {
        FileUtils.deleteDirectory(dir);
      } catch (IOException e) {
        throw new RuntimeException("Can not delete the directory " + dir);
      }
    }
    try {
      FileUtils.forceMkdir(dir);
    } catch (IOException e) {
      throw new RuntimeException("Can not create the directory " + dir);
    }
    return dir;
  }

  /**
   * Checks that a file or a directory is not null and exists.
   */
  public static void assertExists(File file) {
    assertNotNull(file);
    assertThat(file.exists(), is(true));
  }

  public static void assertSimilarXml(String expectedXml, String xml) throws IOException, SAXException {
    Diff diff = isSimilarXml(expectedXml, xml);
    String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
    assertTrue(message, diff.similar());
  }

  static Diff isSimilarXml(String expectedXml, String xml) throws IOException, SAXException {
    XMLUnit.setIgnoreWhitespace(true);
    Diff diff = XMLUnit.compareXML(xml, expectedXml);
    return diff;
  }
}
