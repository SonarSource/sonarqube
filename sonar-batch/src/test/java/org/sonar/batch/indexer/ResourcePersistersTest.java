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
package org.sonar.batch.indexer;

import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.resources.*;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ResourcePersistersTest extends AbstractDbUnitTestCase {

  @Test
  public void getDefaultPersisterForFilesAndPackages() {
    ResourcePersisters persisters = new ResourcePersisters(getSession());


    JavaFile file = new JavaFile("org.foo.Bar");
    assertThat(persisters.get(file), is(DefaultPersister.class));
    assertThat(persisters.get(new JavaPackage("org.foo")), is(DefaultPersister.class));
    assertThat(persisters.get(new File("org/foo/Bar.sql")), is(DefaultPersister.class));

    // always the same instance
    assertTrue(persisters.get(file)==persisters.get(file));
  }

  @Test
  public void getForProjects() {
    ResourcePersisters persisters = new ResourcePersisters(getSession());

    Project project = new Project("my:project");
    assertThat(persisters.get(project), is(ProjectPersister.class));

    // always the same instance
    assertTrue(persisters.get(project)==persisters.get(project));
  }

  @Test
  public void getForLibraries() {
    ResourcePersisters persisters = new ResourcePersisters(getSession());

    Library lib = new Library("commons-lang:commons-lang", "1.0");
    assertThat(persisters.get(lib), is(LibraryPersister.class));

    // always the same instance
    assertTrue(persisters.get(lib)==persisters.get(lib));
  }
}
