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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class ResourcePersisterTest extends AbstractDbUnitTestCase {

  private Project singleProject, multiModuleProject, moduleA, moduleB, moduleB1;

  @Before
  public void before() throws ParseException {
    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
    singleProject = new Project("foo");
    singleProject.setName("Foo").setDescription("some description").setLanguageKey("java").setAnalysisDate(format.parse("25/12/2010"));

    multiModuleProject = new Project("root");
    multiModuleProject.setName("Root").setLanguageKey("java").setAnalysisDate(format.parse("25/12/2010"));

    moduleA = new Project("a");
    moduleA.setName("A").setLanguageKey("java").setAnalysisDate(format.parse("25/12/2010"));
    moduleA.setParent(multiModuleProject);

    moduleB = new Project("b");
    moduleB.setName("B").setLanguageKey("java").setAnalysisDate(format.parse("25/12/2010"));
    moduleB.setParent(multiModuleProject);

    moduleB1 = new Project("b1");
    moduleB1.setName("B1").setLanguageKey("java").setAnalysisDate(format.parse("25/12/2010"));
    moduleB1.setParent(moduleB);
  }


  @Test
  public void shouldSaveNewProject() {
    setupData("shared");

    ResourcePersister persister = new ResourcePersister(getSession());
    persister.saveProject(singleProject);

    checkTables("shouldSaveNewProject", "projects", "snapshots");
  }

  @Test
  public void shouldSaveNewMultiModulesProject() throws ParseException {
    setupData("shared");

    ResourcePersister persister = new ResourcePersister(getSession());
    persister.saveProject(multiModuleProject);
    persister.saveProject(moduleA);
    persister.saveProject(moduleB);
    persister.saveProject(moduleB1);

    checkTables("shouldSaveNewMultiModulesProject", "projects", "snapshots");
  }

  @Test
  public void shouldSaveNewDirectory() {
    setupData("shared");

    ResourcePersister persister = new ResourcePersister(getSession());
    persister.saveProject(singleProject);
    persister.saveResource(singleProject, new JavaPackage("org.foo"));

    // check that the directory is attached to the project
    checkTables("shouldSaveNewDirectory", "projects", "snapshots");
  }

  @Test
  public void shouldSaveNewLibrary() {
    setupData("shared");

    ResourcePersister persister = new ResourcePersister(getSession());
    persister.saveProject(singleProject);
    persister.saveResource(singleProject, new Library("junit:junit", "4.8.2"));
    persister.saveResource(singleProject, new Library("junit:junit", "4.8.2"));// do nothing, already saved
    persister.saveResource(singleProject, new Library("junit:junit", "3.2"));

    checkTables("shouldSaveNewLibrary", "projects", "snapshots");
  }

  @Test
  public void shouldClearResourcesExceptProjects() {
    setupData("shared");

    ResourcePersister persister = new ResourcePersister(getSession());
    persister.saveProject(multiModuleProject);
    persister.saveProject(moduleA);
    persister.saveResource(moduleA, new JavaPackage("org.foo"));
    persister.saveResource(moduleA, new JavaFile("org.foo.MyClass"));
    persister.clear();

    assertThat(persister.getSnapshotsByResource().size(), is(2));
    assertThat(persister.getSnapshotsByResource().get(multiModuleProject), notNullValue());
    assertThat(persister.getSnapshotsByResource().get(moduleA), notNullValue());
  }

  @Test
  public void shouldUpdateExistingResource() {
    setupData("shouldUpdateExistingResource");

    ResourcePersister persister = new ResourcePersister(getSession());
    singleProject.setName("new name");
    singleProject.setDescription("new description");
    persister.saveProject(singleProject);

    checkTables("shouldUpdateExistingResource", "projects", "snapshots");
  }
 
}
