/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.test.TestUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public final class I18nMatchers {

  private I18nMatchers() {
  }

  /**
   * <p>
   * <b>Used by language packs that translate Core bundles.</b>
   * </p>
   * Returns a matcher which checks that a translation bundle is up to date with the corresponding English Core bundle.
   * <ul>
   * <li>If a version of Sonar is specified, then the check is done against this version of the bundle found on Sonar Github repository.</li>
   * <li>If sonarVersion is set to NULL, the check is done against the latest version of this bundle found on Github (master branch).</li>
   * </ul>
   * 
   * @param sonarVersion
   *          the version of the bundle to check against, or NULL to check against the latest source on GitHub
   * @return the matcher
   */
  public static BundleSynchronizedMatcher isBundleUpToDate(String sonarVersion) {
    return new BundleSynchronizedMatcher(sonarVersion);
  }

  /**
   * <p>
   * <b>Used by language packs that translate third-party bundles.</b>
   * </p>
   * Returns a matcher which checks that a translation bundle is up to date with the given reference English bundle from a third-party plugin.
   * 
   * @param referenceEnglishBundleURI
   *          the URI referencing the English bundle to check against
   * @return the matcher
   */
  public static BundleSynchronizedMatcher isBundleUpToDate(URI referenceEnglishBundleURI) {
    return new BundleSynchronizedMatcher(referenceEnglishBundleURI);
  }

  /**
   * <p>
   * <b>Used only by independent plugins that embeds their own bundles for every language.</b>
   * </p>
   * Returns a matcher which checks that a translation bundle is up to date with the corresponding default one found in the same folder.
   * 
   * @return the matcher
   */
  public static BundleSynchronizedMatcher isBundleUpToDate() {
    return new BundleSynchronizedMatcher();
  }

  /**
   * <p>
   * <b>Must be used only by independent plugins that embeds their own bundles for every language.</b>
   * </p>
   * Checks that all the translation bundles found on the classpath are up to date with the corresponding default one found in the same
   * folder.
   */
  public static void assertAllBundlesUpToDate() {
    try {
      assertAllBundlesUpToDate(null, null);
    } catch (URISyntaxException e) {
      // Ignore, this can't happen here
    }
  }

  /**
   * <p>
   * <b>Must be used only by language packs.</b>
   * </p>
   * <p>
   * Depending on the parameters, this method does the following:
   * <ul>
   * <li><b>sonarVersion</b>: checks that all the Core translation bundles found on the classpath are up to date with the corresponding English ones found on Sonar 
   * GitHub repository for the given Sonar version.
   * <ul><li><i>Note: if sonarVersion is set to NULL, the check is done against the latest version of this bundles found the master branch of the GitHub repository.</i></li></ul>
   * </li>
   * <li><b>pluginIdsToBundleUrlMap</b>: checks that other translation bundles found on the classpath are up to date with the reference English bundles of the corresponding
   * plugins given by the "pluginIdsToBundleUrlMap" parameter.
   * </li>
   * </ul>
   * </p>
   * <p><br>
   * The following example will check that the translation of the Core bundles are up to date with version 3.2 of Sonar English Language Pack, and it
   * will also check that the translation of the bundle of the Web plugin is up to date with the reference English bundle of version 1.2 of the Web plugin:
   * <pre>
   * Map<String, String> pluginIdsToBundleUrlMap = Maps.newHashMap();
   * pluginIdsToBundleUrlMap.put("web", "http://svn.codehaus.org/sonar-plugins/tags/sonar-web-plugin-1.2/src/main/resources/org/sonar/l10n/web.properties");
   * assertAllBundlesUpToDate("3.2", pluginIdsToBundleUrlMap);
   * </pre>
   * </p>
   * 
   * @param sonarVersion
   *          the version of the bundles to check against, or NULL to check against the latest source on GitHub
   * @param pluginIdsToBundleUrlMap
   *          a map that gives, for a given plugin, the URL of the English bundle that must be used to check the translation.
   * @throws URISyntaxException if the provided URLs in the "pluginIdsToBundleUrlMap" parameter are not correct
   */
  public static void assertAllBundlesUpToDate(String sonarVersion, Map<String, String> pluginIdsToBundleUrlMap) throws URISyntaxException {
    File bundleFolder = TestUtils.getResource(BundleSynchronizedMatcher.L10N_PATH);
    if (bundleFolder == null || !bundleFolder.isDirectory()) {
      fail("No bundle found in '" + BundleSynchronizedMatcher.L10N_PATH + "'");
    }

    Collection<File> bundles = FileUtils.listFiles(bundleFolder, new String[] {"properties"}, false);
    Map<String, String> failedAssertionMessages = Maps.newHashMap();
    for (File bundle : bundles) {
      String bundleName = bundle.getName();
      if (bundleName.indexOf('_') > 0) {
        try {
          String baseBundleName = BundleSynchronizedMatcher.extractDefaultBundleName(bundleName);
          String pluginId = StringUtils.substringBefore(baseBundleName, ".");
          if (BundleSynchronizedMatcher.isCoreBundle(baseBundleName)) {
            // this is a core bundle => must be checked againt the provided version of Sonar
            assertThat(bundleName, isBundleUpToDate(sonarVersion));
          } else if (pluginIdsToBundleUrlMap != null && pluginIdsToBundleUrlMap.get(pluginId) != null) {
            // this is a third-party plugin translated by a language pack => must be checked against the provided URL
            assertThat(bundleName, isBundleUpToDate(new URI(pluginIdsToBundleUrlMap.get(pluginId))));
          } else {
            // this is the case of a plugin that provides all the bundles for every language => check the bundles inside the plugin
            assertThat(bundleName, isBundleUpToDate());
          }
        } catch (AssertionError e) {
          failedAssertionMessages.put(bundleName, e.getMessage());
        }
      }
    }

    if (!failedAssertionMessages.isEmpty()) {
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
