/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.api;

import org.junit.Test;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JavaClassTest {

  @Test
  public void shouldCreateReferenceFromName() {
    JavaClass javaClass = JavaClass.create("org.foo.Bar");
    assertThat(javaClass.getClassName(), is("Bar"));
    assertThat(javaClass.getKey(), is("org.foo.Bar"));
    assertThat(javaClass.getLanguage(), is((Language)Java.INSTANCE));
    assertThat(javaClass.getName(), is("org.foo.Bar"));
    assertThat(javaClass.getLongName(), is("org.foo.Bar"));
  }

  @Test
  public void shouldCreateReferenceFromPackageAndClassname() {
    JavaClass javaClass = JavaClass.create("org.foo", "Bar");
    assertThat(javaClass.getClassName(), is("Bar"));
    assertThat(javaClass.getKey(), is("org.foo.Bar"));
    assertThat(javaClass.getLanguage(), is((Language)Java.INSTANCE));
    assertThat(javaClass.getName(), is("org.foo.Bar"));
    assertThat(javaClass.getLongName(), is("org.foo.Bar"));
  }

  @Test
  public void shouldGetPackageName() {
    JavaClass javaClass = JavaClass.create("org.foo.Bar");
    assertThat(javaClass.getPackageName(), is("org.foo"));

    javaClass = JavaClass.create("Bar");
    assertThat(javaClass.getPackageName(), is(""));
  }

  @Test
  public void shouldGetClassName() {
    JavaClass javaClass = JavaClass.create("org.foo.Bar");
    assertThat(javaClass.getClassName(), is("Bar"));

    javaClass = JavaClass.create("Bar");
    assertThat(javaClass.getClassName(), is("Bar"));
  }

  @Test
  public void shouldOverrideToString() {
    JavaClass javaClass = JavaClass.create("org.foo.Bar");
    assertThat(javaClass.toString(), is("org.foo.Bar"));
  }

  @Test
  public void shouldBuild() {
    JavaClass javaClass = new JavaClass.Builder().setName("org.foo", "Bar").setFromLine(30).create();
    assertThat(javaClass.getName(), is("org.foo.Bar"));
    assertThat(javaClass.getFromLine(), is(30));
    assertThat(javaClass.getToLine(), is(JavaClass.UNKNOWN_LINE));
  }

  @Test
  public void shouldNotBuildWithNegativeNumberOfLine() {
    JavaClass javaClass = new JavaClass.Builder().setName("org.foo", "Bar").setFromLine(-30).setToLine(0).create();
    assertThat(javaClass.getFromLine(), is(JavaClass.UNKNOWN_LINE));
    assertThat(javaClass.getToLine(), is(0));
  }
}
