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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonar.test.i18n.I18nMatchers.isBundleUpToDate;

import java.io.File;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.sonar.test.TestUtils;

public class BundleSynchronizedTest {

  private static final String GITHUB_RAW_FILE_PATH = "https://raw.github.com/SonarSource/sonar/master/sonar-testing-harness/src/test/resources/org/sonar/l10n/";
  private BundleSynchronizedMatcher matcher;

  @Before
  public void test() throws Exception {
    matcher = new BundleSynchronizedMatcher(null);
  }

  @Test
  // The case of a Sonar plugin that embeds all the bundles for every language
  public void testBundlesInsideSonarPlugin() {
    // synchronized bundle
    assertThat("myPlugin_fr_CA.properties", isBundleUpToDate());
    assertFalse(new File("target/l10n/myPlugin_fr_CA.properties.report.txt").exists());
    // missing keys
    try {
      assertThat("myPlugin_fr.properties", isBundleUpToDate());
      assertTrue(new File("target/l10n/myPlugin_fr.properties.report.txt").exists());
    } catch (AssertionError e) {
      assertThat(e.getMessage(), containsString("Missing keys are:\n\t- second.prop"));
    }
    // unnecessary many keys
    try {
      assertThat("myPlugin_fr_QB.properties", isBundleUpToDate());
      assertTrue(new File("target/l10n/myPlugin_fr_QB.properties.report.txt").exists());
    } catch (AssertionError e) {
      assertThat(e.getMessage(), containsString("The following keys do not exist in the default bundle:\n\t- fourth.prop"));
    }
  }

  @Test
  // The case of a Sonar Language Pack that translates the Core bundles
  public void testBundlesOfLanguagePack() {
    // synchronized bundle
    assertThat("core_fr_CA.properties", new BundleSynchronizedMatcher(null, GITHUB_RAW_FILE_PATH));
    // missing keys
    try {
      assertThat("core_fr.properties", new BundleSynchronizedMatcher(null, GITHUB_RAW_FILE_PATH));
    } catch (AssertionError e) {
      assertThat(e.getMessage(), containsString("Missing keys are:\n\t- second.prop"));
    }
  }

  @Test
  public void testGetBundleFileFromClasspath() {
    matcher.getBundleFileFromClasspath("core_fr.properties");
    try {
      matcher.getBundleFileFromClasspath("unexistingBundle.properties");
    } catch (AssertionError e) {
      assertThat(e.getMessage(), containsString("File 'unexistingBundle.properties' does not exist in '/org/sonar/l10n/'."));
    }
  }

  @Test
  public void testGetBundleFileFromGithub() throws Exception {
    matcher = new BundleSynchronizedMatcher(null, GITHUB_RAW_FILE_PATH);
    matcher.getBundleFileFromGithub("core.properties");
    assertTrue(new File("target/l10n/download/core.properties").exists());
  }

  @Test
  public void testExtractDefaultBundleName() throws Exception {
    assertThat(matcher.extractDefaultBundleName("myPlugin_fr.properties"), is("myPlugin.properties"));
    assertThat(matcher.extractDefaultBundleName("myPlugin_fr_QB.properties"), is("myPlugin.properties"));
    try {
      matcher.extractDefaultBundleName("myPlugin.properties");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          containsString("The bundle 'myPlugin.properties' is a default bundle (without locale), so it can't be compared."));
    }
  }

  @Test
  public void testIsCoreBundle() throws Exception {
    assertTrue(matcher.isCoreBundle("core.properties"));
    assertFalse(matcher.isCoreBundle("myPlugin.properties"));
  }

  @Test
  public void testRetrieveMissingKeys() throws Exception {
    File defaultBundle = TestUtils.getResource(BundleSynchronizedMatcher.L10N_PATH + "myPlugin.properties");
    File frBundle = TestUtils.getResource(BundleSynchronizedMatcher.L10N_PATH + "myPlugin_fr.properties");
    File qbBundle = TestUtils.getResource(BundleSynchronizedMatcher.L10N_PATH + "myPlugin_fr_QB.properties");

    Collection<String> diffs = matcher.retrieveMissingKeys(frBundle, defaultBundle);
    assertThat(diffs.size(), is(1));
    assertThat(diffs, hasItem("second.prop"));

    diffs = matcher.retrieveMissingKeys(qbBundle, defaultBundle);
    assertThat(diffs.size(), is(0));
  }

  @Test
  public void testComputeGitHubURL() throws Exception {
    assertThat(
        matcher.computeGitHubURL("core.properties", null),
        is("https://raw.github.com/SonarSource/sonar/master/plugins/sonar-l10n-en-plugin/src/main/resources/org/sonar/l10n/core.properties"));
    assertThat(
        matcher.computeGitHubURL("core.properties", "2.11-SNAPSHOT"),
        is("https://raw.github.com/SonarSource/sonar/master/plugins/sonar-l10n-en-plugin/src/main/resources/org/sonar/l10n/core.properties"));
    assertThat(matcher.computeGitHubURL("core.properties", "2.10"),
        is("https://raw.github.com/SonarSource/sonar/2.10/plugins/sonar-l10n-en-plugin/src/main/resources/org/sonar/l10n/core.properties"));
  }
}
