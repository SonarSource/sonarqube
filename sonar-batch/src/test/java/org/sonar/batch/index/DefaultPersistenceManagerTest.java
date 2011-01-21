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
package org.sonar.batch.index;

import org.junit.Test;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.java.api.JavaClass;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultPersistenceManagerTest {
  
  @Test
  public void shouldPersistResoucesWithScopeHigherThanFile() {
    assertThat(DefaultPersistenceManager.isPersistable(new File("Foo.java")), is(true));
    assertThat(DefaultPersistenceManager.isPersistable(new Directory("bar/Foo.java")), is(true));
    assertThat(DefaultPersistenceManager.isPersistable(new Project("foo")), is(true));
    assertThat(DefaultPersistenceManager.isPersistable(new Library("foo", "1.2")), is(true));
  }

  @Test
  public void shouldNotPersistResoucesWithScopeLowerThanFile() {
    assertThat(DefaultPersistenceManager.isPersistable(JavaClass.createRef("com.foo.Bar")), is(false));
  }
}
