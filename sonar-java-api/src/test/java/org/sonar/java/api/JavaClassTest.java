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

package org.sonar.java.api;

import org.junit.Test;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JavaClassTest {

  @Test
  public void shouldCreateReferenceFromName() {
    JavaClass javaClass = JavaClass.createRef("org.foo.Bar");
    assertThat(javaClass.getClassName(), is("Bar"));
    assertThat(javaClass.getKey(), is("org.foo.Bar"));
    assertThat(javaClass.getLanguage(), is((Language)Java.INSTANCE));
    assertThat(javaClass.getName(), is("org.foo.Bar"));
    assertThat(javaClass.getLongName(), is("org.foo.Bar"));
  }

  @Test
  public void shouldCreateReferenceFromPackageAndClassname() {
    JavaClass javaClass = JavaClass.createRef("org.foo", "Bar");
    assertThat(javaClass.getClassName(), is("Bar"));
    assertThat(javaClass.getKey(), is("org.foo.Bar"));
    assertThat(javaClass.getLanguage(), is((Language)Java.INSTANCE));
    assertThat(javaClass.getName(), is("org.foo.Bar"));
    assertThat(javaClass.getLongName(), is("org.foo.Bar"));
  }

  @Test
  public void shouldGetPackageName() {
    JavaClass javaClass = JavaClass.createRef("org.foo.Bar");
    assertThat(javaClass.getPackageName(), is("org.foo"));

    javaClass = JavaClass.createRef("Bar");
    assertThat(javaClass.getPackageName(), is(""));
  }

  @Test
  public void shouldGetClassName() {
    JavaClass javaClass = JavaClass.createRef("org.foo.Bar");
    assertThat(javaClass.getClassName(), is("Bar"));

    javaClass = JavaClass.createRef("Bar");
    assertThat(javaClass.getClassName(), is("Bar"));
  }
}
