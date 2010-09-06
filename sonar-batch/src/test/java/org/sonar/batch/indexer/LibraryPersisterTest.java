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

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class LibraryPersisterTest extends AbstractDbUnitTestCase {

  private Bucket<Project> projectBucket;
  private LibraryPersister persister;

  @Before
  public void before() throws ParseException {
    persister = new LibraryPersister(getSession(), new SimpleDateFormat("yyyy-MM-dd HH:mm").parse( "2010-05-18 17:00"));
  }

  @Test
  public void createLibrary() throws Exception {
    setup("createLibrary");

    Library library = new Library("commons-lang:commons-lang", "1.1")
        .setName("Commons Lang");

    Bucket<Library> bucket = new Bucket<Library>(library).setProject(projectBucket);
    persister.persist(bucket);

    check("createLibrary", "projects", "snapshots");
  }

  @Test
  public void reuseExistingLibrary() throws Exception {
    setup("reuseExistingLibrary");

    Library library = new Library("commons-lang:commons-lang", "1.1")
        .setName("Commons Lang");

    Bucket<Library> bucket = new Bucket<Library>(library).setProject(projectBucket);
    persister.persist(bucket);

    check("reuseExistingLibrary", "projects", "snapshots");
  }

  @Test
  public void addNewLibraryVersion() throws Exception {
    setup("addNewLibraryVersion");

    Library library = new Library("commons-lang:commons-lang", "1.2")
        .setName("Commons Lang");

    Bucket<Library> bucket = new Bucket<Library>(library).setProject(projectBucket);
    persister.persist(bucket);

    check("addNewLibraryVersion", "projects", "snapshots");
  }

  private void setup(String unitTest) throws Exception {
    setupData(unitTest);

    Project project = new Project("my:project");
    project.setId(1);
    projectBucket = new Bucket<Project>(project);
    projectBucket.setSnapshot(getSession().getSingleResult(Snapshot.class, "id", 1));
  }

  private void check(String unitTest, String... tables) {
    getSession().commit();
    checkTables(unitTest, tables);
  }
}
