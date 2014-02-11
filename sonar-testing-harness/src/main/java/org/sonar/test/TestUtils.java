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
package org.sonar.test;

import com.google.common.base.Throwables;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * Utilities for unit tests
 *
 * @since 2.2
 */
public final class TestUtils {

  private TestUtils() {
  }

  /**
   * Search for a test resource in the classpath. For example getResource("org/sonar/MyClass/foo.txt");
   *
   * @param path the starting slash is optional
   * @return the resource. Null if resource not found
   */
  public static File getResource(String path) {
    String resourcePath = path;
    if (!resourcePath.startsWith("/")) {
      resourcePath = "/" + resourcePath;
    }
    URL url = TestUtils.class.getResource(resourcePath);
    if (url != null) {
      return FileUtils.toFile(url);
    }
    return null;
  }

  public static String getResourceContent(String path) {
    URL url = TestUtils.class.getResource(path);
    if (url == null) {
      return null;
    }

    try {
      return IOUtils.toString(url, Charsets.UTF_8);
    } catch (IOException e) {
      throw new SonarException("Can not load the resource: " + path, e);
    }
  }

  /**
   * Search for a resource in the classpath. For example calling the method getResource(getClass(), "myTestName/foo.txt") from
   * the class org.sonar.Foo loads the file $basedir/src/test/resources/org/sonar/Foo/myTestName/foo.txt
   *
   * @return the resource. Null if resource not found
   */
  public static File getResource(Class baseClass, String path) {
    String resourcePath = StringUtils.replaceChars(baseClass.getCanonicalName(), '.', '/');
    if (!path.startsWith("/")) {
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
   *
   * @param baseClass the unit test class
   * @param testName  the test name
   * @param clean     remove all the sub-directories and files ?
   */
  public static File getTestTempDir(Class baseClass, String testName, boolean clean) {
    File dir = new File("target/test-tmp/" + baseClass.getCanonicalName() + "/" + testName);
    if (clean && dir.exists()) {
      try {
        FileUtils.deleteDirectory(dir);
      } catch (IOException e) {
        throw new SonarException("Can not delete the directory " + dir, e);
      }
    }
    try {
      FileUtils.forceMkdir(dir);
    } catch (IOException e) {
      throw new SonarException("Can not create the directory " + dir, e);
    }
    return dir;
  }

  public static void assertSimilarXml(String expectedXml, String xml) {
    Diff diff = isSimilarXml(expectedXml, xml);
    String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
    assertTrue(message, diff.similar());
  }

  static Diff isSimilarXml(String expectedXml, String xml) {
    XMLUnit.setIgnoreWhitespace(true);
    try {
      return XMLUnit.compareXML(xml, expectedXml);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
