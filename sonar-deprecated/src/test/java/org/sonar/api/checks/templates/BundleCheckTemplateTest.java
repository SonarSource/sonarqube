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
package org.sonar.api.checks.templates;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.checks.samples.AnnotatedCheckWithBundles;
import org.sonar.api.checks.samples.SimpleAnnotatedCheck;

import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class BundleCheckTemplateTest {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  @BeforeClass
  public static void beforeAll() {
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterClass
  public static void afterAll() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @Test
  public void loadBundlesFromClass() {
    BundleCheckTemplate check = new BundleCheckTemplate("key", AnnotatedCheckWithBundles.class);

    assertNotNull(check.getBundle(Locale.ENGLISH));
    assertNotNull(check.getBundle(Locale.FRENCH));
    assertNotNull(check.getBundle(Locale.CHINESE)); // use the english bundle

    assertThat(check.getBundle(Locale.ENGLISH).getString("title"), is("I18n Check"));
    assertThat(check.getBundle(Locale.CHINESE).getString("title"), is("I18n Check"));
    assertThat(check.getBundle(Locale.FRENCH).getString("title"), is("Règle d'internationalisation"));
  }

  @Test
  public void useDefaultValuesWhenNoBundles() {
    BundleCheckTemplate check = new BundleCheckTemplate("key", SimpleAnnotatedCheck.class);
    check.setDefaultTitle("default title");
    check.setDefaultDescription("default desc");

    assertThat(check.getTitle(null), is("default title"));
    assertThat(check.getTitle(Locale.ENGLISH), is("default title"));
    assertThat(check.getTitle(Locale.CHINESE), is("default title"));

    assertThat(check.getDescription(Locale.ENGLISH), is("default desc"));
    assertThat(check.getDescription(Locale.CHINESE), is("default desc"));
  }
}
