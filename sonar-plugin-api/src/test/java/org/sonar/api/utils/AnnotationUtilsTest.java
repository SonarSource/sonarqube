/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationUtilsTest {

  @Test
  public void getClassAnnotation() {
    FakeAnnotation annotation = AnnotationUtils.getAnnotation(new SuperClass(), FakeAnnotation.class);
    assertThat(annotation.value()).isEqualTo("foo");
  }

  @Test
  public void getClassAnnotationWithDeprecatedMethod() {
    FakeAnnotation annotation = AnnotationUtils.getClassAnnotation(new SuperClass(), FakeAnnotation.class);
    assertThat(annotation.value()).isEqualTo("foo");
  }

  @Test
  public void searchClassAnnotationInSuperClass() {
    FakeAnnotation annotation = AnnotationUtils.getAnnotation(new ChildClass(), FakeAnnotation.class);
    assertThat(annotation.value()).isEqualTo("foo");
  }

  @Test
  public void searchClassAnnotationInInterface() {
    FakeAnnotation annotation = AnnotationUtils.getAnnotation(new ImplementedClass(), FakeAnnotation.class);
    assertThat(annotation.value()).isEqualTo("foo");
  }

  @Test
  public void noClassAnnotation() {
    FakeAnnotation annotation = AnnotationUtils.getAnnotation("a string", FakeAnnotation.class);
    assertThat(annotation).isNull();
  }

  @Test
  public void shouldAcceptClasses() {
    FakeAnnotation annotation = AnnotationUtils.getAnnotation(SuperClass.class, FakeAnnotation.class);
    assertThat(annotation.value()).isEqualTo("foo");

    annotation = AnnotationUtils.getAnnotation(ChildClass.class, FakeAnnotation.class);
    assertThat(annotation.value()).isEqualTo("foo");
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

@FakeAnnotation("foo")
interface AnnotatedInterface {
}

abstract class AbstractClass implements AnnotatedInterface {
  
}
class ImplementedClass extends AbstractClass {

}
