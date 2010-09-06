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
package org.sonar.check;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AnnotationIntrospectorTest {

  @Test
  public void defaultCheckKeyIsClassname() {
    assertThat(AnnotationIntrospector.getCheckKey(SimplestCheck.class), is(SimplestCheck.class.getCanonicalName()));
  }

  @Test
  public void checkKeyCanBeOverriden() {
    assertThat(AnnotationIntrospector.getCheckKey(CheckWithOverridenKey.class), is("overridenKey"));
  }

  @Test
  public void noProperties() {
    assertThat(AnnotationIntrospector.getPropertyFields(SimplestCheck.class).size(), is(0));
  }

  @Test
  public void getProperties() {
    List<Field> fields = AnnotationIntrospector.getPropertyFields(CheckWithProperties.class);
    assertThat(fields.size(), is(2));

    assertThat(fields.get(0).getName(), is("active"));
    assertThat(AnnotationIntrospector.getPropertyFieldKey(fields.get(0)), is("active"));

    assertThat(fields.get(1).getName(), is("max"));
    assertThat(AnnotationIntrospector.getPropertyFieldKey(fields.get(1)), is("Maximum"));
  }


  @Test
  public void getCheckAnnotation() {
    assertNotNull(AnnotationIntrospector.getCheckAnnotation(SimplestCheck.class));
    assertNull(AnnotationIntrospector.getCheckAnnotation(String.class));
  }
}


@Check(isoCategory = IsoCategory.Portability, priority = Priority.CRITICAL)
class SimplestCheck {

}

@Check(key = "overridenKey", isoCategory = IsoCategory.Portability, priority = Priority.CRITICAL)
class CheckWithOverridenKey {

}

@Check(isoCategory = IsoCategory.Portability, priority = Priority.CRITICAL)
class CheckWithProperties {

  @CheckProperty
  private boolean active = false;

  @CheckProperty(key = "Maximum")
  private int max = 50;

  private String nonProperty;
}