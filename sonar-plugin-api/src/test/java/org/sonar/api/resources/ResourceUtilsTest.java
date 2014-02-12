/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.resources;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceUtilsTest {

  @Test
  public void checkFile() {
    File file = new File("hello.Foo");
    assertThat(ResourceUtils.isClass(file), is(true));
    assertThat(ResourceUtils.isPackage(file), is(false));
    assertThat(ResourceUtils.isModuleProject(file), is(false));
    assertThat(ResourceUtils.isSpace(file), is(false));
    assertThat(ResourceUtils.isEntity(file), is(true));
    assertThat(ResourceUtils.isSet(file), is(false));
    assertThat(ResourceUtils.isRootProject(file), is(false));
    assertThat(ResourceUtils.isUnitTestClass(file), is(false));
  }

  @Test
  public void checkUnitTest() {
    File utFile = new File("hello.Foo");
    utFile.setQualifier(Qualifiers.UNIT_TEST_FILE);
    assertThat(ResourceUtils.isClass(utFile), is(false));
    assertThat(ResourceUtils.isPackage(utFile), is(false));
    assertThat(ResourceUtils.isModuleProject(utFile), is(false));
    assertThat(ResourceUtils.isSpace(utFile), is(false));
    assertThat(ResourceUtils.isEntity(utFile), is(true));
    assertThat(ResourceUtils.isSet(utFile), is(false));
    assertThat(ResourceUtils.isRootProject(utFile), is(false));
    assertThat(ResourceUtils.isUnitTestClass(utFile), is(true));
  }

  @Test
  public void checkDirectory() {
    Directory dir = new Directory("hello");
    assertThat(ResourceUtils.isClass(dir), is(false));
    assertThat(ResourceUtils.isPackage(dir), is(true));
    assertThat(ResourceUtils.isModuleProject(dir), is(false));
    assertThat(ResourceUtils.isSpace(dir), is(true));
    assertThat(ResourceUtils.isEntity(dir), is(false));
    assertThat(ResourceUtils.isSet(dir), is(false));
    assertThat(ResourceUtils.isRootProject(dir), is(false));
    assertThat(ResourceUtils.isUnitTestClass(dir), is(false));
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
