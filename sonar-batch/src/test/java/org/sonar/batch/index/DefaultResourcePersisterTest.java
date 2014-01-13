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
package org.sonar.batch.index;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultResourcePersisterTest extends AbstractDbUnitTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  Project singleProject, singleCopyProject, multiModuleProject, moduleA, moduleB, moduleB1, existingProject;
  SnapshotCache snapshotCache = mock(SnapshotCache.class);
  ResourceCache resourceCache = mock(ResourceCache.class);

  @Before
  public void before() throws ParseException {
    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
    singleProject = newProject("foo", "java");
    singleProject.setName("Foo").setDescription("some description").setAnalysisDate(format.parse("25/12/2010"));

    existingProject = newProject("my:key", "java");
    existingProject.setName("Other project").setDescription("some description").setAnalysisDate(format.parse("25/12/2010"));

    singleCopyProject = newCopyProject("foo", "java", 10);
    singleCopyProject.setName("Foo").setDescription("some description").setAnalysisDate(format.parse("25/12/2010"));

    multiModuleProject = newProject("root", "java");
    multiModuleProject.setName("Root").setAnalysisDate(format.parse("25/12/2010"));

    moduleA = newProject("a", "java");
    moduleA.setName("A").setAnalysisDate(format.parse("25/12/2010"));
    moduleA.setParent(multiModuleProject);
    moduleA.setPath("/moduleA");

    moduleB = newProject("b", "java");
    moduleB.setName("B").setAnalysisDate(format.parse("25/12/2010"));
    moduleB.setParent(multiModuleProject);
    moduleB.setPath("/moduleB");

    moduleB1 = newProject("b1", "java");
    moduleB1.setName("B1").setAnalysisDate(format.parse("25/12/2010"));
    moduleB1.setParent(moduleB);
    moduleB1.setPath("/moduleB1");
  }

  @Test
  public void shouldSaveNewProject() {
    setupData("shared");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    persister.saveProject(singleProject, null);

    checkTables("shouldSaveNewProject", new String[] {"build_date", "created_at"}, "projects", "snapshots");

    // SONAR-3636 : created_at must be fed when inserting a new entry in the 'projects' table
    ResourceModel model = getSession().getSingleResult(ResourceModel.class, "key", singleProject.getKey());
    assertThat(model.getCreatedAt()).isNotNull();
  }

  @Test
  public void shouldSaveCopyProject() {
    setupData("shared");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    persister.saveProject(singleCopyProject, null);

    checkTables("shouldSaveCopyProject", new String[] {"build_date", "created_at"}, "projects", "snapshots");
  }

  @Test
  public void shouldSaveNewMultiModulesProject() {
    setupData("shared");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    persister.saveProject(multiModuleProject, null);
    persister.saveProject(moduleA, multiModuleProject);
    persister.saveProject(moduleB, multiModuleProject);
    persister.saveProject(moduleB1, moduleB);

    checkTables("shouldSaveNewMultiModulesProject", new String[] {"build_date", "created_at"}, "projects", "snapshots");
  }

  // SONAR-4245
  @Test
  public void shouldFailWhenTryingToConvertProjectIntoModule() {
    setupData("shared");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    existingProject.setParent(multiModuleProject);
    persister.saveProject(multiModuleProject, null);

    thrown.expect(SonarException.class);
    thrown.expectMessage("The project 'my:key' is already defined in SonarQube but not as a module of project 'root'. "
      + "If you really want to stop directly analysing project 'my:key', please first delete it from SonarQube and then relaunch the analysis of project 'root'.");

    persister.saveProject(existingProject, multiModuleProject);
  }

  @Test
  public void shouldSaveNewDirectory() {
    setupData("shared");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    persister.saveProject(singleProject, null);
    persister.saveResource(singleProject,
      Directory.create("src/main/java/org/foo", "org.foo").setEffectiveKey("foo:/src/main/java/org/foo").setDeprecatedEffectiveKey("foo:org.foo"));

    // check that the directory is attached to the project
    checkTables("shouldSaveNewDirectory", new String[] {"build_date", "created_at"}, "projects", "snapshots");
  }

  @Test
  public void shouldSaveNewLibrary() {
    setupData("shared");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    persister.saveProject(singleProject, null);
    persister.saveResource(singleProject, new Library("junit:junit", "4.8.2").setEffectiveKey("junit:junit"));
    persister.saveResource(singleProject, new Library("junit:junit", "4.8.2").setEffectiveKey("junit:junit"));// do nothing, already saved
    persister.saveResource(singleProject, new Library("junit:junit", "3.2").setEffectiveKey("junit:junit"));

    checkTables("shouldSaveNewLibrary", new String[] {"build_date", "created_at"}, "projects", "snapshots");
  }

  @Test
  public void shouldClearResourcesExceptProjects() {
    setupData("shared");

    DefaultResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    persister.saveProject(multiModuleProject, null);
    persister.saveProject(moduleA, multiModuleProject);
    persister.saveResource(moduleA, new JavaPackage("org.foo").setEffectiveKey("a:org.foo"));
    persister.saveResource(moduleA, new JavaFile("org.foo.MyClass").setEffectiveKey("a:org.foo.MyClass"));
    persister.clear();

    assertThat(persister.getSnapshotsByResource().size(), is(2));
    assertThat(persister.getSnapshotsByResource().get(multiModuleProject), notNullValue());
    assertThat(persister.getSnapshotsByResource().get(moduleA), notNullValue());
  }

  @Test
  public void shouldUpdateExistingResource() {
    setupData("shouldUpdateExistingResource");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    singleProject.setName("new name");
    singleProject.setDescription("new description");
    persister.saveProject(singleProject, null);

    checkTables("shouldUpdateExistingResource", new String[] {"build_date", "created_at"}, "projects", "snapshots");
  }

  // SONAR-1700
  @Test
  public void shouldRemoveRootIndexIfResourceIsProject() {
    setupData("shouldRemoveRootIndexIfResourceIsProject");

    ResourcePersister persister = new DefaultResourcePersister(getSession(), mock(ResourcePermissions.class), snapshotCache, resourceCache);
    persister.saveProject(singleProject, null);

    checkTables("shouldRemoveRootIndexIfResourceIsProject", new String[] {"build_date", "created_at"}, "projects", "snapshots");
  }

  @Test
  public void shouldGrantDefaultPermissionsIfNewProject() {
    setupData("shared");

    ResourcePermissions permissions = mock(ResourcePermissions.class);
    when(permissions.hasRoles(singleProject)).thenReturn(false);

    ResourcePersister persister = new DefaultResourcePersister(getSession(), permissions, snapshotCache, resourceCache);
    persister.saveProject(singleProject, null);

    verify(permissions).grantDefaultRoles(singleProject);
  }

  @Test
  public void shouldNotGrantDefaultPermissionsIfExistingProject() {
    setupData("shared");

    ResourcePermissions permissions = mock(ResourcePermissions.class);
    when(permissions.hasRoles(singleProject)).thenReturn(true);

    ResourcePersister persister = new DefaultResourcePersister(getSession(), permissions, snapshotCache, resourceCache);
    persister.saveProject(singleProject, null);

    verify(permissions, never()).grantDefaultRoles(singleProject);
  }

  private static Project newProject(String key, String language) {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty("sonar.language", language);
    return new Project(key).setConfiguration(configuration).setAnalysisType(Project.AnalysisType.DYNAMIC);
  }

  private static Project newCopyProject(String key, String language, int copyResourceId) {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty("sonar.language", language);
    return new CopyProject(key, copyResourceId).setConfiguration(configuration).setAnalysisType(Project.AnalysisType.DYNAMIC);
  }

  private static class CopyProject extends Project implements ResourceCopy {

    private int copyResourceId;

    public CopyProject(String key, int copyResourceId) {
      super(key);
      this.copyResourceId = copyResourceId;
    }

    public int getCopyResourceId() {
      return copyResourceId;
    }

  }

}
