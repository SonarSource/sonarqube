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
package org.sonar.api.resources;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class JavaTest {

  @Test
  public void test() {
    Java language = new Java();
    assertThat(language.getFileSuffixes()).isEqualTo(new String[] {".java", ".jav"});

    assertThat(Java.isJavaFile(new java.io.File("Example.java"))).isTrue();
    assertThat(Java.isJavaFile(new java.io.File("Example.jav"))).isTrue();
    assertThat(Java.isJavaFile(new java.io.File("Example.notjava"))).isFalse();
  }

  @Test
  public void should_be_equal_to_another_java_language_implementation() {
    Java java = new Java();
    Java2 otherJavaLanguage = new Java2();

    assertThat(java).isEqualTo(otherJavaLanguage);
  }

  static class Java2 extends AbstractLanguage {
    public Java2() {
      super("java");
    }

    public String[] getFileSuffixes() {
      return new String[0];
    }
  }
}
