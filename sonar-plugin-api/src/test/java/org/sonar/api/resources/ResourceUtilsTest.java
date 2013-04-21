/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceUtilsTest {

  @Test
  public void checkJavaClass() {
    JavaFile clazz = new JavaFile("hello.Foo");
    assertThat(ResourceUtils.isClass(clazz), is(true));
    assertThat(ResourceUtils.isPackage(clazz), is(false));
    assertThat(ResourceUtils.isModuleProject(clazz), is(false));
    assertThat(ResourceUtils.isSpace(clazz), is(false));
    assertThat(ResourceUtils.isEntity(clazz), is(true));
    assertThat(ResourceUtils.isSet(clazz), is(false));
    assertThat(ResourceUtils.isRootProject(clazz), is(false));
    assertThat(ResourceUtils.isUnitTestClass(clazz), is(false));
  }

  @Test
  public void checkJavaUnitTest() {
    JavaFile clazz = new JavaFile("hello.Foo", true);
    assertThat(ResourceUtils.isClass(clazz), is(false));
    assertThat(ResourceUtils.isPackage(clazz), is(false));
    assertThat(ResourceUtils.isModuleProject(clazz), is(false));
    assertThat(ResourceUtils.isSpace(clazz), is(false));
    assertThat(ResourceUtils.isEntity(clazz), is(true));
    assertThat(ResourceUtils.isSet(clazz), is(false));
    assertThat(ResourceUtils.isRootProject(clazz), is(false));
    assertThat(ResourceUtils.isUnitTestClass(clazz), is(true));
  }

  @Test
  public void checkJavaPackage() {
    JavaPackage pack = new JavaPackage("hello");
    assertThat(ResourceUtils.isClass(pack), is(false));
    assertThat(ResourceUtils.isPackage(pack), is(true));
    assertThat(ResourceUtils.isModuleProject(pack), is(false));
    assertThat(ResourceUtils.isSpace(pack), is(true));
    assertThat(ResourceUtils.isEntity(pack), is(false));
    assertThat(ResourceUtils.isSet(pack), is(false));
    assertThat(ResourceUtils.isRootProject(pack), is(false));
    assertThat(ResourceUtils.isUnitTestClass(pack), is(false));
  }

  @Test
  public void shouldBePersistable() {
    assertThat(ResourceUtils.isPersistable(new File("Foo.java")), is(true));
    assertThat(ResourceUtils.isPersistable(new Directory("bar/Foo.java")), is(true));
    assertThat(ResourceUtils.isPersistable(new Project("foo")), is(true));
    assertThat(ResourceUtils.isPersistable(new Library("foo", "1.2")), is(true));
  }

  @Test
  public void shouldNotBePersistable() {
    Resource javaClass = mock(Resource.class);
    when(javaClass.getScope()).thenReturn(Scopes.PROGRAM_UNIT);
    Resource javaMethod = mock(Resource.class);
    when(javaMethod.getScope()).thenReturn(Scopes.BLOCK_UNIT);

    assertThat(ResourceUtils.isPersistable(javaClass), is(false));
    assertThat(ResourceUtils.isPersistable(javaMethod), is(false));
  }
}
