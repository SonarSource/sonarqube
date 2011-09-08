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
package org.sonar.test.i18n;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.test.TestUtils;

import com.google.common.collect.Maps;

public final class I18nMatchers {

  private I18nMatchers() {
  }

  public static BundleSynchronizedMatcher isBundleUpToDate() {
    return new BundleSynchronizedMatcher();
  }

  public static void assertAllBundlesUpToDate() {
    File bundleFolder = TestUtils.getResource(BundleSynchronizedMatcher.L10N_PATH);
    if (bundleFolder == null || !bundleFolder.isDirectory()) {
      fail("No bundle found in '" + BundleSynchronizedMatcher.L10N_PATH + "'");
    }

    Collection<File> bundles = FileUtils.listFiles(bundleFolder, new String[] { "properties" }, false);
    Map<String, String> failedAssertionMessages = Maps.newHashMap();
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

    if ( !failedAssertionMessages.isEmpty()) {
      StringBuilder message = new StringBuilder();
      message.append(failedAssertionMessages.size());
      message.append(" bundles are not up-to-date: ");
      message.append(StringUtils.join(failedAssertionMessages.keySet(), ", "));
      message.append("\n\n");
      message.append(StringUtils.join(failedAssertionMessages.values(), "\n\n"));
      fail(message.toString());
    }

  }

}
