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
package org.sonar.api.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class AnnotationUtilsTest {

  @Test
  public void getClassAnnotation() {
    FakeAnnotation annotation = AnnotationUtils.getClassAnnotation(new SuperClass(), FakeAnnotation.class);
    assertThat(annotation.value(), is("foo"));
  }

  @Test
  public void searchClassAnnotationInSuperClass() {
    FakeAnnotation annotation = AnnotationUtils.getClassAnnotation(new ChildClass(), FakeAnnotation.class);
    assertThat(annotation.value(), is("foo"));
  }

  @Test
  public void noClassAnnotation() {
    FakeAnnotation annotation = AnnotationUtils.getClassAnnotation("a string", FakeAnnotation.class);
    assertThat(annotation, nullValue());
  }

  @Test
  public void shouldAcceptClasses() {
    FakeAnnotation annotation = AnnotationUtils.getClassAnnotation(SuperClass.class, FakeAnnotation.class);
    assertThat(annotation.value(), is("foo"));

    annotation = AnnotationUtils.getClassAnnotation(ChildClass.class, FakeAnnotation.class);
    assertThat(annotation.value(), is("foo"));
  }

}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
    @interface FakeAnnotation {
  String value();
}

@FakeAnnotation("foo")
class SuperClass {
}

class ChildClass extends SuperClass {

}