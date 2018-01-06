/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.test.i18n;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.sonar.test.TestUtils;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public final class I18nMatchers {

  private I18nMatchers() {
  }

  /**
   * Returns a matcher which checks that a translation bundle is up to date with the corresponding default one found in the classpath.
   *
   * @return the matcher
   */
  public static BundleSynchronizedMatcher isBundleUpToDate() {
    return new BundleSynchronizedMatcher();
  }

  /**
   * Checks that all the translation bundles found on the classpath are up to date with the corresponding default ones found in the classpath.
   */
  public static void assertBundlesUpToDate() {
    File bundleFolder = getResource(BundleSynchronizedMatcher.L10N_PATH);
    if (bundleFolder == null || !bundleFolder.isDirectory()) {
      fail("No bundle found in: " + BundleSynchronizedMatcher.L10N_PATH);
    }

    Collection<File> bundles = FileUtils.listFiles(bundleFolder, new String[] {"properties"}, false);
    Map<String, String> failedAssertionMessages = new HashMap<>();
    for (File bundle : bundles) {
      String bundleName = bundle.getName();
      if (bundleName.indexOf('_') > 0) {
        try {
          assertThat(bundleName, isBundleUpToDate());
        } catch (AssertionError e) {
          failedAssertionMessages.put(bundleName, e.getMessage());
        }
      }
    }

    if (!failedAssertionMessages.isEmpty()) {
      StringBuilder message = new StringBuilder();
      message.append(failedAssertionMessages.size());
      message.append(" bundles are not up-to-date: ");
      message.append(String.join(", ", failedAssertionMessages.keySet()));
      message.append("\n\n");
      message.append(String.join("\n\n", failedAssertionMessages.values()));
      fail(message.toString());
    }
  }

  private static File getResource(String path) {
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
}
