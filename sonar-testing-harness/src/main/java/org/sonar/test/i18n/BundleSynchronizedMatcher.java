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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.sonar.test.TestUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BundleSynchronizedMatcher extends BaseMatcher<String> {

  public static final String L10N_PATH = "/org/sonar/l10n/";
  private static final String GITHUB_RAW_FILE_PATH = "https://raw.github.com/SonarSource/sonar/master/plugins/sonar-l10n-en-plugin/src/main/resources/org/sonar/l10n/";
  private static final Collection<String> CORE_BUNDLES = Lists.newArrayList("checkstyle.properties", "core.properties",
    "findbugs.properties", "gwt.properties", "pmd.properties", "squidjava.properties");

  private String sonarVersion;
  // we use this variable to be able to unit test this class without looking at the real Github core bundles that change all the time
  private String remote_file_path;
  private String bundleName;
  private SortedMap<String, String> missingKeys;
  private SortedMap<String, String> additionalKeys;

  public BundleSynchronizedMatcher(String sonarVersion) {
    this(sonarVersion, GITHUB_RAW_FILE_PATH);
  }

  public BundleSynchronizedMatcher(String sonarVersion, String remote_file_path) {
    this.sonarVersion = sonarVersion;
    this.remote_file_path = remote_file_path;
  }

  public boolean matches(Object arg0) {
    if (!(arg0 instanceof String)) {
      return false;
    }
    bundleName = (String) arg0;

    File bundle = getBundleFileFromClasspath(bundleName);

    // Find the default bundle name which should be compared to
    String defaultBundleName = extractDefaultBundleName(bundleName);
    File defaultBundle;
    if (isCoreBundle(defaultBundleName)) {
      defaultBundle = getBundleFileFromGithub(defaultBundleName);
    } else {
      defaultBundle = getBundleFileFromClasspath(defaultBundleName);
    }

    // and now let's compare
    try {
      missingKeys = retrieveMissingTranslations(bundle, defaultBundle);
      additionalKeys = retrieveMissingTranslations(defaultBundle, bundle);
      return missingKeys.isEmpty();
    } catch (IOException e) {
      fail("An error occured while reading the bundles: " + e.getMessage());
      return false;
    }
  }

  public void describeTo(Description description) {
    // report file
    File dumpFile = new File("target/l10n/" + bundleName + ".report.txt");

    // prepare message
    StringBuilder details = prepareDetailsMessage(dumpFile);
    description.appendText(details.toString());

    // print report in target directory
    printReport(dumpFile, details.toString());
  }

  private StringBuilder prepareDetailsMessage(File dumpFile) {
    StringBuilder details = new StringBuilder("\n=======================\n'");
    details.append(bundleName);
    details.append("' is not up-to-date.");
    print("\n\n Missing translations are:", missingKeys, details);
    print("\n\nThe following translations do not exist in the reference bundle:", additionalKeys, details);
    details.append("\n\nSee report file located at: " + dumpFile.getAbsolutePath());
    details.append("\n=======================");
    return details;
  }

  private void print(String title, SortedMap<String, String> translations, StringBuilder to) {
    if (!translations.isEmpty()) {
      to.append(title);
      for (Map.Entry<String, String> entry : translations.entrySet()) {
        to.append("\n").append(entry.getKey()).append("=").append(entry.getValue());
      }
    }
  }

  private void printReport(File dumpFile, String details) {
    if (dumpFile.exists()) {
      dumpFile.delete();
    }
    dumpFile.getParentFile().mkdirs();
    FileWriter writer = null;
    try {
      writer = new FileWriter(dumpFile);
      writer.write(details);
    } catch (IOException e) {
      throw new RuntimeException("Unable to write the report to 'target/l10n/" + bundleName + ".report.txt'");
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  protected SortedMap<String, String> retrieveMissingTranslations(File bundle, File referenceBundle) throws IOException {
    SortedMap<String, String> missingKeys = Maps.newTreeMap();

    Properties bundleProps = loadProperties(bundle);
    Properties referenceProperties = loadProperties(referenceBundle);

    for (Map.Entry<Object, Object> entry : referenceProperties.entrySet()) {
      String key = (String) entry.getKey();
      if (!bundleProps.containsKey(key)) {
        missingKeys.put(key, (String) entry.getValue());
      }
    }

    return missingKeys;
  }

  private Properties loadProperties(File f) throws IOException {
    Properties props = new Properties();
    FileInputStream input = new FileInputStream(f);
    try {
      props.load(input);
      return props;

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  protected File getBundleFileFromGithub(String defaultBundleName) {
    File localBundle = new File("target/l10n/download/" + defaultBundleName);
    try {
      String remoteFile = computeGitHubURL(defaultBundleName, sonarVersion);
      saveUrlToLocalFile(remoteFile, localBundle);
    } catch (MalformedURLException e) {
      fail("Could not download the original core bundle at: " + remote_file_path + defaultBundleName);
    } catch (IOException e) {
      fail("Could not download the original core bundle at: " + remote_file_path + defaultBundleName);
    }
    assertThat("File 'target/tmp/" + defaultBundleName + "' has been downloaded but does not exist.", localBundle, notNullValue());
    assertThat("File 'target/tmp/" + defaultBundleName + "' has been downloaded but does not exist.", localBundle.exists(), is(true));
    return localBundle;
  }

  protected String computeGitHubURL(String defaultBundleName, String sonarVersion) {
    String computedURL = remote_file_path + defaultBundleName;
    if (sonarVersion != null && !sonarVersion.contains("-SNAPSHOT")) {
      computedURL = computedURL.replace("/master/", "/" + sonarVersion + "/");
    }
    return computedURL;
  }

  protected File getBundleFileFromClasspath(String bundleName) {
    File bundle = TestUtils.getResource(L10N_PATH + bundleName);
    assertThat("File '" + bundleName + "' does not exist in '/org/sonar/l10n/'.", bundle, notNullValue());
    assertThat("File '" + bundleName + "' does not exist in '/org/sonar/l10n/'.", bundle.exists(), is(true));
    return bundle;
  }

  protected String extractDefaultBundleName(String bundleName) {
    int firstUnderScoreIndex = bundleName.indexOf('_');
    assertThat("The bundle '" + bundleName + "' is a default bundle (without locale), so it can't be compared.", firstUnderScoreIndex > 0,
      is(true));
    return bundleName.substring(0, firstUnderScoreIndex) + ".properties";
  }

  protected boolean isCoreBundle(String defaultBundleName) {
    return CORE_BUNDLES.contains(defaultBundleName);
  }

  private void saveUrlToLocalFile(String url, File localFile) throws IOException {
    if (localFile.exists()) {
      localFile.delete();
    }
    localFile.getParentFile().mkdirs();

    InputStream in = null;
    OutputStream fout = null;
    try {
      in = new BufferedInputStream(new URL(url).openStream());
      fout = new FileOutputStream(localFile);

      byte data[] = new byte[1024];
      int count;
      while ((count = in.read(data, 0, 1024)) != -1) {
        fout.write(data, 0, count);
      }
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(fout);
    }
  }

}
