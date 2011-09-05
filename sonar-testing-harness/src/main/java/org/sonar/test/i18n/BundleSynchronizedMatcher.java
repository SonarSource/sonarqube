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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.sonar.test.TestUtils;

import com.google.common.collect.Lists;

public class BundleSynchronizedMatcher extends BaseMatcher<String> {

  public static final String L10N_PATH = "/org/sonar/l10n/";
  private static final String GITHUB_RAW_FILE_PATH = "https://raw.github.com/SonarSource/sonar/master/plugins/sonar-l10n-en-plugin/src/main/resources/org/sonar/l10n/";
  private static final Collection<String> CORE_BUNDLES = Lists.newArrayList("checkstyle.properties", "core.properties",
      "findbugs.properties", "gwt.properties", "pmd.properties", "squidjava.properties");

  // we use this variable to be able to unit test this class without looking at the real Github core bundles that change all the time
  private String remote_file_path;
  private String bundleName;
  private Collection<String> missingKeys;
  private Collection<String> nonExistingKeys;

  public BundleSynchronizedMatcher() {
    this(GITHUB_RAW_FILE_PATH);
  }

  public BundleSynchronizedMatcher(String remote_file_path) {
    this.remote_file_path = remote_file_path;
  }

  public boolean matches(Object arg0) {
    if ( !(arg0 instanceof String)) {
      return false;
    }
    bundleName = (String) arg0;

    // Get the bundle
    File bundle = getBundleFileFromClasspath(bundleName);

    // Find the default bundle name which should be compared to
    String defaultBundleName = extractDefaultBundleName(bundleName);
    File defaultBundle = null;
    if (isCoreBundle(defaultBundleName)) {
      defaultBundle = getBundleFileFromGithub(defaultBundleName);
    } else {
      defaultBundle = getBundleFileFromClasspath(defaultBundleName);
    }

    // and now let's compare
    try {
      missingKeys = retrieveMissingKeys(bundle, defaultBundle);
      nonExistingKeys = retrieveMissingKeys(defaultBundle, bundle);
      return missingKeys.isEmpty() && nonExistingKeys.isEmpty();
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
    details.append("' is not synchronized.");
    if ( !missingKeys.isEmpty()) {
      details.append("\n\n Missing keys are:");
      for (String key : missingKeys) {
        details.append("\n\t- " + key);
      }
    }
    if ( !nonExistingKeys.isEmpty()) {
      details.append("\n\nThe following keys do not exist in the default bundle:");
      for (String key : nonExistingKeys) {
        details.append("\n\t- " + key);
      }
    }
    details.append("\n\nSee report file located at: " + dumpFile.getAbsolutePath());
    details.append("\n=======================");
    return details;
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
      System.out.println("Unable to write the report to 'target/l10n/" + bundleName + ".report.txt'.");
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  protected Collection<String> retrieveMissingKeys(File bundle, File defaultBundle) throws IOException {
    Collection<String> missingKeys = Lists.newArrayList();

    Properties bundleProps = new Properties();
    bundleProps.load(new FileInputStream(bundle));
    Set<Object> bundleKeys = bundleProps.keySet();

    Properties defaultBundleProps = new Properties();
    defaultBundleProps.load(new FileInputStream(defaultBundle));
    Set<Object> defaultBundleKeys = defaultBundleProps.keySet();

    for (Object key : defaultBundleKeys) {
      if ( !bundleKeys.contains(key)) {
        missingKeys.add(key.toString());
      }
    }

    return missingKeys;
  }

  protected File getBundleFileFromGithub(String defaultBundleName) {
    File bundle = new File("target/l10n/download/" + defaultBundleName);
    try {
      saveUrl(remote_file_path + defaultBundleName, bundle);
    } catch (MalformedURLException e) {
      fail("Could not download the original core bundle at: " + remote_file_path + defaultBundleName);
    } catch (IOException e) {
      fail("Could not download the original core bundle at: " + remote_file_path + defaultBundleName);
    }
    assertThat("File 'target/tmp/" + defaultBundleName + "' has been downloaded but does not exist.", bundle, notNullValue());
    assertThat("File 'target/tmp/" + defaultBundleName + "' has been downloaded but does not exist.", bundle.exists(), is(true));
    return bundle;
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

  private void saveUrl(String url, File localFile) throws MalformedURLException, IOException {
    if (localFile.exists()) {
      localFile.delete();
    }
    localFile.getParentFile().mkdirs();

    BufferedInputStream in = null;
    FileOutputStream fout = null;
    try {
      in = new BufferedInputStream(new URL(url).openStream());
      fout = new FileOutputStream(localFile);

      byte data[] = new byte[1024];
      int count;
      while ((count = in.read(data, 0, 1024)) != -1) {
        fout.write(data, 0, count);
      }
    } finally {
      if (in != null)
        in.close();
      if (fout != null)
        fout.close();
    }
  }

}
