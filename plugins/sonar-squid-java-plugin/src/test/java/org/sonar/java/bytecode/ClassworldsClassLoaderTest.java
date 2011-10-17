/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.java.bytecode;

import org.codehaus.classworlds.ClassWorld;
import org.junit.Test;
import org.sonar.java.ast.SquidTestUtils;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ClassworldsClassLoaderTest {

  @Test
  public void createManyTimes() {
    // check that the method create() can be executed more than once
    assertThat(ClassworldsClassLoader.create(Collections.<File>emptyList()), not(nullValue()));
    assertThat(ClassworldsClassLoader.create(Collections.<File>emptyList()), not(nullValue()));
  }

  /**
   * See SONAR-2824:
   * ClassLoader created by {@link ClassworldsClassLoader},
   * should be able to load classes only from JDK and from provided list of JAR-files,
   * thus it shouldn't be able to load class {@link ClassWorld}.
   */
  @Test
  public void shouldBeIsolated() throws ClassNotFoundException {
    ClassLoader classloader = ClassworldsClassLoader.create(Collections.EMPTY_LIST);
    try {
      classloader.loadClass(ClassWorld.class.getName());
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }
    assertThat(classloader.loadClass("java.lang.Integer"), not(nullValue()));
    assertThat(classloader.getResource("java/lang/Integer.class"), not(nullValue()));
  }

  @Test
  public void createFromDirectory() throws ClassNotFoundException {
    File dir = SquidTestUtils.getFile("/bytecode/bin/");
    ClassLoader classloader = ClassworldsClassLoader.create(dir);
    assertThat(classloader.loadClass("tags.TagName"), not(nullValue()));
    assertThat(classloader.getResource("tags/TagName.class"), not(nullValue()));

    try {
      classloader.loadClass("tags.Unknown");
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }
  }

  @Test
  public void createFromJar() throws ClassNotFoundException {
    File jar = SquidTestUtils.getFile("/bytecode/lib/hello.jar");
    ClassLoader classloader = ClassworldsClassLoader.create(jar);
    assertThat(classloader.loadClass("org.sonar.tests.Hello"), not(nullValue()));
    assertThat(classloader.getResource("org/sonar/tests/Hello.class"), not(nullValue()));

    try {
      classloader.loadClass("foo.Unknown");
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }
  }

  @Test
  public void unknownJarIsIgnored() throws ClassNotFoundException {
    File jar = SquidTestUtils.getFile("/bytecode/lib/unknown.jar");
    ClassLoader classloader = ClassworldsClassLoader.create(jar);
    assertThat(classloader.getResource("org/sonar/tests/Hello.class"), nullValue());
  }
}
