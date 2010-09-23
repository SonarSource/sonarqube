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
package org.sonar.api.checks.checkers;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.check.Check;
import org.sonar.check.CheckProperty;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnnotationCheckerFactoryTest {

  private AnnotationCheckerFactory factory = null;

  @Before
  public void before() {
    CheckProfile profile = new CheckProfile("test", "java");
    factory = new AnnotationCheckerFactory(profile, "repository", Arrays.asList(
        CheckerWithoutProperties.class,
        CheckerWithStringProperty.class,
        CheckerWithPrimitiveProperties.class));

  }

  @Test
  public void createCheckerWithoutProperties() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckerWithoutProperties.class);
    CheckerWithoutProperties checker = (CheckerWithoutProperties) factory.instantiate(check, CheckerWithoutProperties.class);
    assertNotNull(checker);
  }

  @Test
  public void createCheckerWithStringProperty() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckerWithStringProperty.class);

    Map map = new HashMap();
    map.put("max", "foo");
    when(check.getProperties()).thenReturn(map);

    CheckerWithStringProperty checker = (CheckerWithStringProperty) factory.instantiate(check, CheckerWithStringProperty.class);
    assertNotNull(checker);
    assertThat(checker.getMax(), is("foo"));
  }

  @Test(expected = UnvalidCheckerException.class)
  public void failIfMissingProperty() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckerWithStringProperty.class);

    Map map = new HashMap();
    map.put("max", "foo");
    map.put("missing", "bar");
    when(check.getProperties()).thenReturn(map);

    factory.instantiate(check, CheckerWithStringProperty.class);
  }

  @Test
  public void createCheckerWithPrimitiveProperties() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckerWithPrimitiveProperties.class);

    Map map = new HashMap();
    map.put("max", "300");
    map.put("active", "true");
    when(check.getProperties()).thenReturn(map);

    CheckerWithPrimitiveProperties checker = (CheckerWithPrimitiveProperties) factory.instantiate(check, CheckerWithPrimitiveProperties.class);
    assertNotNull(checker);
    assertThat(checker.getMax(), is(300));
    assertThat(checker.isActive(), is(true));
  }

  @Test
  public void createCheckerWithIntegerProperty() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckerWithIntegerProperty.class);

    Map map = new HashMap();
    map.put("max", "300");
    when(check.getProperties()).thenReturn(map);

    CheckerWithIntegerProperty checker = (CheckerWithIntegerProperty) factory.instantiate(check, CheckerWithIntegerProperty.class);
    assertNotNull(checker);
    assertThat(checker.getMax(), is(300));
  }

  @Test
  public void createCheckerWithDefaultValues() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckerWithPrimitiveProperties.class);
    when(check.getProperties()).thenReturn(new HashMap<String, String>());

    CheckerWithPrimitiveProperties checker = (CheckerWithPrimitiveProperties) factory.instantiate(check, CheckerWithPrimitiveProperties.class);
    assertNotNull(checker);
    assertThat(checker.getMax(), is(50));
    assertThat(checker.isActive(), is(false));
  }

  @Test(expected=UnvalidCheckerException.class)
  public void checkWithUnsupportedPropertyType() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckWithUnsupportedPropertyType.class);
    Map map = new HashMap();
    map.put("max", "300");
    when(check.getProperties()).thenReturn(map);

    factory.instantiate(check, CheckWithUnsupportedPropertyType.class);
  }

  @Test
  @Ignore("Key is not used as i18n is not managed on properties.")
  public void createCheckerWithOverridenPropertyKey() {
    org.sonar.api.checks.profiles.Check check = mockCheck(CheckWithOverridenPropertyKey.class);
    Map map = new HashMap();
    map.put("maximum", "300");
    when(check.getProperties()).thenReturn(map);

    CheckWithOverridenPropertyKey checker = (CheckWithOverridenPropertyKey) factory.instantiate(check, CheckWithOverridenPropertyKey.class);
    assertNotNull(checker);
    assertThat(checker.getMax(), is(300));
  }


  private org.sonar.api.checks.profiles.Check mockCheck(Class checkerClass) {
    org.sonar.api.checks.profiles.Check check = mock(org.sonar.api.checks.profiles.Check.class);
    when(check.getRepositoryKey()).thenReturn("repository");
    when(check.getTemplateKey()).thenReturn(checkerClass.getCanonicalName());
    return check;
  }
}

@Check(isoCategory = IsoCategory.Efficiency, priority = Priority.CRITICAL)
class CheckWithOverridenPropertyKey{

  @CheckProperty(key = "maximum")
  private int max = 50;

  public int getMax() {
    return max;
  }
}