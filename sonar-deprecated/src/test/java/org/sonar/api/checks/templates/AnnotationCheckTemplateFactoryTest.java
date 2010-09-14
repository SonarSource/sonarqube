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
import org.sonar.api.checks.samples.*;
import org.sonar.check.IsoCategory;

import java.util.Iterator;
import java.util.Locale;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class AnnotationCheckTemplateFactoryTest {

  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
  private static final Locale ALTERNATIVE_LOCALE = Locale.FRENCH;
  private static final Locale UNKNOWN_LOCALE = Locale.CHINESE;

  private static final Locale JVM_LOCALE = Locale.getDefault();

  @BeforeClass
  public static void beforeAll() {
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterClass
  public static void afterAll() {
    Locale.setDefault(JVM_LOCALE);
  }

  @Test
  public void checkWithDefaultValues() {
    BundleCheckTemplate check = new AnnotationCheckTemplateFactory(null).create(SimpleAnnotatedCheck.class);
    assertNotNull(check);

    assertThat(check.getKey(), is("org.sonar.api.checks.samples.SimpleAnnotatedCheck"));

    assertThat(check.getTitle(DEFAULT_LOCALE), is("org.sonar.api.checks.samples.SimpleAnnotatedCheck"));
    assertThat(check.getTitle(ALTERNATIVE_LOCALE), is("org.sonar.api.checks.samples.SimpleAnnotatedCheck"));
    assertThat(check.getTitle(UNKNOWN_LOCALE), is("org.sonar.api.checks.samples.SimpleAnnotatedCheck"));

    assertThat(check.getDescription(DEFAULT_LOCALE), is(""));
    assertThat(check.getDescription(ALTERNATIVE_LOCALE), is(""));
    assertThat(check.getDescription(UNKNOWN_LOCALE), is(""));

    assertEquals(IsoCategory.Efficiency, check.getIsoCategory());

    assertThat(check.getProperties().size(), is(2));
    Iterator<CheckTemplateProperty> it = check.getProperties().iterator();

    CheckTemplateProperty maxTemplateProperty = it.next();
    assertThat(maxTemplateProperty.getKey(), is("max"));

    assertThat(maxTemplateProperty.getDescription(DEFAULT_LOCALE), is(""));
    assertThat(maxTemplateProperty.getDescription(ALTERNATIVE_LOCALE), is(""));
    assertThat(maxTemplateProperty.getDescription(UNKNOWN_LOCALE), is(""));

    CheckTemplateProperty minTemplateProperty = it.next();
    assertThat(minTemplateProperty.getKey(), is("min"));
  }

  @Test
  public void failOnNonCheckClass() {
    assertNull(new AnnotationCheckTemplateFactory(null).create(String.class));
  }

  @Test
  public void checkWithDetailedMessages() {
    BundleCheckTemplate check = new AnnotationCheckTemplateFactory(null).create(DetailedAnnotatedCheck.class);
    assertNotNull(check);

    assertThat(check.getKey(), is("org.sonar.api.checks.samples.DetailedAnnotatedCheck"));

    assertThat(check.getTitle(DEFAULT_LOCALE), is("Detailed Check"));
    assertThat(check.getTitle(ALTERNATIVE_LOCALE), is("Detailed Check"));
    assertThat(check.getTitle(UNKNOWN_LOCALE), is("Detailed Check"));

    assertThat(check.getDescription(DEFAULT_LOCALE), is("Detailed description"));
    assertThat(check.getDescription(ALTERNATIVE_LOCALE), is("Detailed description"));
    assertThat(check.getDescription(UNKNOWN_LOCALE), is("Detailed description"));

    assertThat(check.getIsoCategory(), is(IsoCategory.Reliability));

    assertThat(check.getProperties().size(), is(2));
    Iterator<CheckTemplateProperty> it = check.getProperties().iterator();

    CheckTemplateProperty maxTemplateProperty = it.next();
    assertThat(maxTemplateProperty.getKey(), is("max"));

    assertThat(maxTemplateProperty.getDescription(DEFAULT_LOCALE), is("Maximum value"));
    assertThat(maxTemplateProperty.getDescription(ALTERNATIVE_LOCALE), is("Maximum value"));
    assertThat(maxTemplateProperty.getDescription(UNKNOWN_LOCALE), is("Maximum value"));
  }

  @Test
  public void checkWithInternationalizedMessages() {
    BundleCheckTemplate check = new AnnotationCheckTemplateFactory(null).create(AnnotatedCheckWithBundles.class);
    assertNotNull(check);

    assertThat(check.getKey(), is("org.sonar.api.checks.samples.AnnotatedCheckWithBundles"));
    assertThat(check.getTitle(DEFAULT_LOCALE), is("I18n Check"));
    assertThat(check.getTitle(ALTERNATIVE_LOCALE), is("Règle d'internationalisation"));
    assertThat(check.getTitle(UNKNOWN_LOCALE), is("I18n Check"));

    assertThat(check.getDescription(DEFAULT_LOCALE), is("Description in english"));
    assertThat(check.getDescription(ALTERNATIVE_LOCALE), is("Description en Français"));
    assertThat(check.getDescription(UNKNOWN_LOCALE), is("Description in english"));

    assertThat(check.getProperties().size(), is(2));
    Iterator<CheckTemplateProperty> it = check.getProperties().iterator();

    CheckTemplateProperty maxTemplateProperty = it.next();
    assertThat(maxTemplateProperty.getKey(), is("max"));

    assertThat(maxTemplateProperty.getDescription(DEFAULT_LOCALE), is("Description in english of the maximum value"));
    assertThat(maxTemplateProperty.getDescription(ALTERNATIVE_LOCALE), is("Description en Français de la valeur maximale"));
    assertThat(maxTemplateProperty.getDescription(UNKNOWN_LOCALE), is("Description in english of the maximum value"));
  }

  @Test
  public void loadBundlesFromAlternativePath() {
    BundleCheckTemplate check = new AnnotationCheckTemplateFactory(null).create(I18nCheckWithAlternativeBundle.class);
    assertNotNull(check);

    assertThat(check.getKey(), is("new_key"));
    assertThat(check.getTitle(DEFAULT_LOCALE), is("Alternative Path to Bundle"));
  }

  @Test
  public void loadFromAnnotationIfNoDefaultLocale() {
    BundleCheckTemplate check = new AnnotationCheckTemplateFactory(null).create(I18nCheckWithoutDefaultLocale.class);
    assertNotNull(check);

    assertThat(check.getTitle(DEFAULT_LOCALE), is("Title from annotation"));
    assertThat(check.getTitle(ALTERNATIVE_LOCALE), is("Titre depuis le bundle"));
  }
}
