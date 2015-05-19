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
package org.sonar.api.resources;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceUtilsTest {

  @Test
  public void checkFile() {
    File file = File.create("hello.Foo");
    assertThat(ResourceUtils.isClass(file)).isFalse();
    assertThat(ResourceUtils.isPackage(file)).isFalse();
    assertThat(ResourceUtils.isModuleProject(file)).isFalse();
    assertThat(ResourceUtils.isSpace(file)).isFalse();
    assertThat(ResourceUtils.isEntity(file)).isTrue();
    assertThat(ResourceUtils.isSet(file)).isFalse();
    assertThat(ResourceUtils.isRootProject(file)).isFalse();
    assertThat(ResourceUtils.isUnitTestClass(file)).isFalse();
  }

  @Test
  public void checkUnitTest() {
    File utFile = File.create("hello.Foo");
    utFile.setQualifier(Qualifiers.UNIT_TEST_FILE);
    assertThat(ResourceUtils.isClass(utFile)).isFalse();
    assertThat(ResourceUtils.isPackage(utFile)).isFalse();
    assertThat(ResourceUtils.isModuleProject(utFile)).isFalse();
    assertThat(ResourceUtils.isSpace(utFile)).isFalse();
    assertThat(ResourceUtils.isEntity(utFile)).isTrue();
    assertThat(ResourceUtils.isSet(utFile)).isFalse();
    assertThat(ResourceUtils.isRootProject(utFile)).isFalse();
    assertThat(ResourceUtils.isUnitTestClass(utFile)).isTrue();
  }

  @Test
  public void checkDirectory() {
    Directory dir = Directory.create("hello");
    assertThat(ResourceUtils.isClass(dir)).isFalse();
    assertThat(ResourceUtils.isPackage(dir)).isFalse();
    assertThat(ResourceUtils.isModuleProject(dir)).isFalse();
    assertThat(ResourceUtils.isSpace(dir)).isTrue();
    assertThat(ResourceUtils.isEntity(dir)).isFalse();
    assertThat(ResourceUtils.isSet(dir)).isFalse();
    assertThat(ResourceUtils.isRootProject(dir)).isFalse();
    assertThat(ResourceUtils.isUnitTestClass(dir)).isFalse();
  }

  @Test
  public void shouldBePersistable() {
    assertThat(ResourceUtils.isPersistable(File.create("Foo.java"))).isTrue();
    assertThat(ResourceUtils.isPersistable(Directory.create("bar/Foo.java"))).isTrue();
    assertThat(ResourceUtils.isPersistable(new Project("foo"))).isTrue();
  }

  @Test
  public void shouldNotBePersistable() {
    Resource javaClass = mock(Resource.class);
    when(javaClass.getScope()).thenReturn(Scopes.PROGRAM_UNIT);
    Resource javaMethod = mock(Resource.class);
    when(javaMethod.getScope()).thenReturn(Scopes.BLOCK_UNIT);

    assertThat(ResourceUtils.isPersistable(javaClass)).isFalse();
    assertThat(ResourceUtils.isPersistable(javaMethod)).isFalse();
  }
}
