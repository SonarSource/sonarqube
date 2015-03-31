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
package org.sonar.batch.index;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.*;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.component.db.ComponentMapper;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import javax.persistence.Query;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.batch.index.ResourcePersister.MODULE_UUID_PATH_SEPARATOR;

public class ResourcePersisterTest extends AbstractDbUnitTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Project singleProject, singleCopyProject, multiModuleProject, moduleA, moduleB, moduleB1, existingProject;
  private ResourceCache resourceCache;

  private ResourcePersister persister;

  private ProjectTree projectTree;

  private ResourcePermissions permissions;

  @Before
  public void before() throws ParseException {
    resourceCache = new ResourceCache();

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

    projectTree = mock(ProjectTree.class);
    permissions = mock(ResourcePermissions.class);
    persister = new ResourcePersister(projectTree, getSession(), permissions, resourceCache, mock(ScanGraph.class));
  }

  @Test
  public void shouldSaveNewProject() {
    setupData("shared");

    persister.persist(null, singleProject, null);

    checkTables("shouldSaveNewProject", new String[] {"build_date", "created_at", "authorization_updated_at", "uuid", "project_uuid", "module_uuid", "module_uuid_path"},
      "projects", "snapshots");

    // Need to enable snapshot to make resource visible using ComponentMapper
    enableSnapshot(1001);
    try (SqlSession session = getMyBatis().openSession(false)) {
      ComponentDto newProject = session.getMapper(ComponentMapper.class).selectByKey("foo");
      assertThat(newProject.uuid()).isNotNull();
      assertThat(newProject.projectUuid()).isEqualTo(newProject.uuid());
      assertThat(newProject.moduleUuid()).isNull();
      assertThat(newProject.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + newProject.uuid() + MODULE_UUID_PATH_SEPARATOR);
      // SONAR-3636 : created_at must be fed when inserting a new entry in the 'projects' table
      assertThat(newProject.getCreatedAt()).isNotNull();
    }
  }

  @Test
  public void shouldSaveCopyProject() {
    setupData("shared");

    persister.persist(null, singleCopyProject, null);

    checkTables("shouldSaveCopyProject", new String[] {"build_date", "created_at", "authorization_updated_at", "uuid", "project_uuid", "module_uuid", "module_uuid_path"},
      "projects", "snapshots");
    // Need to enable snapshot to make resource visible using ComponentMapper
    enableSnapshot(1001);
    try (SqlSession session = getMyBatis().openSession(false)) {
      ComponentDto newProject = session.getMapper(ComponentMapper.class).selectByKey("foo");
      assertThat(newProject.uuid()).isNotNull();
      assertThat(newProject.projectUuid()).isEqualTo(newProject.uuid());
      assertThat(newProject.moduleUuid()).isNull();
      assertThat(newProject.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + newProject.uuid() + MODULE_UUID_PATH_SEPARATOR);
    }
  }

  @Test
  public void shouldSaveNewMultiModulesProject() {
    setupData("shared");

    resourceCache.add(multiModuleProject, null).setSnapshot(persister.persist(null, multiModuleProject, null));
    resourceCache.add(moduleA, multiModuleProject).setSnapshot(persister.persist(null, moduleA, multiModuleProject));
    resourceCache.add(moduleB, multiModuleProject).setSnapshot(persister.persist(null, moduleB, multiModuleProject));
    resourceCache.add(moduleB1, moduleB).setSnapshot(persister.persist(null, moduleB1, moduleB));
    Resource file = File.create("src/main/java/org/Foo.java").setEffectiveKey("b1:src/main/java/org/Foo.java");
    file.getParent().setEffectiveKey("b1:src/main/java/org");
    resourceCache.add(file.getParent(), moduleB1).setSnapshot(persister.persist(moduleB1, file.getParent(), null));
    persister.persist(moduleB1, file, file.getParent());

    checkTables("shouldSaveNewMultiModulesProject",
      new String[] {"build_date", "created_at", "authorization_updated_at", "uuid", "project_uuid", "module_uuid", "module_uuid_path"}, "projects", "snapshots");

    // Need to enable snapshot to make resource visible using ComponentMapper
    enableSnapshot(1001);
    enableSnapshot(1002);
    enableSnapshot(1003);
    enableSnapshot(1004);
    enableSnapshot(1005);
    enableSnapshot(1006);
    try (SqlSession session = getMyBatis().openSession(false)) {
      ComponentDto root = session.getMapper(ComponentMapper.class).selectByKey("root");
      assertThat(root.uuid()).isNotNull();
      assertThat(root.projectUuid()).isEqualTo(root.uuid());
      assertThat(root.moduleUuid()).isNull();
      assertThat(root.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto a = session.getMapper(ComponentMapper.class).selectByKey("a");
      assertThat(a.uuid()).isNotNull();
      assertThat(a.projectUuid()).isEqualTo(root.uuid());
      assertThat(a.moduleUuid()).isEqualTo(root.uuid());
      assertThat(a.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + a.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto b = session.getMapper(ComponentMapper.class).selectByKey("b");
      assertThat(b.uuid()).isNotNull();
      assertThat(b.projectUuid()).isEqualTo(root.uuid());
      assertThat(b.moduleUuid()).isEqualTo(root.uuid());
      assertThat(b.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto b1 = session.getMapper(ComponentMapper.class).selectByKey("b1");
      assertThat(b1.uuid()).isNotNull();
      assertThat(b1.projectUuid()).isEqualTo(root.uuid());
      assertThat(b1.moduleUuid()).isEqualTo(b.uuid());
      assertThat(b1.moduleUuidPath()).isEqualTo(
        MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR + b1.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto dir = session.getMapper(ComponentMapper.class).selectByKey("b1:src/main/java/org");
      assertThat(dir.uuid()).isNotNull();
      assertThat(dir.projectUuid()).isEqualTo(root.uuid());
      assertThat(dir.moduleUuid()).isEqualTo(b1.uuid());
      assertThat(dir.moduleUuidPath()).isEqualTo(
        MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR + b1.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto fileComp = session.getMapper(ComponentMapper.class).selectByKey("b1:src/main/java/org/Foo.java");
      assertThat(fileComp.uuid()).isNotNull();
      assertThat(fileComp.projectUuid()).isEqualTo(root.uuid());
      assertThat(fileComp.moduleUuid()).isEqualTo(b1.uuid());
      assertThat(fileComp.moduleUuidPath()).isEqualTo(
        MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR + b1.uuid() + MODULE_UUID_PATH_SEPARATOR);
    }
  }

  // FIXME this is a kind of medium test
  @Test
  public void shouldSaveNewMultiModulesProjectUsingIndex() throws IOException {
    setupData("shared");

    java.io.File baseDir = temp.newFolder();

    when(projectTree.getRootProject()).thenReturn(multiModuleProject);
    when(projectTree.getProjectDefinition(multiModuleProject)).thenReturn(ProjectDefinition.create().setBaseDir(baseDir));
    when(projectTree.getProjectDefinition(moduleA)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleA")));
    when(projectTree.getProjectDefinition(moduleB)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleB")));
    when(projectTree.getProjectDefinition(moduleB1)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleB/moduleB1")));

    DefaultIndex index = new DefaultIndex(resourceCache, null, projectTree, mock(MetricFinder.class),
      mock(ResourceKeyMigration.class),
      mock(MeasureCache.class));

    index.start();

    Resource file = File.create("src/main/java/org/Foo.java");

    index.setCurrentProject(moduleB1, null);
    index.index(file);

    // Emulate another project having library dependency on moduleA
    index.addResource(new Library(moduleA.getKey(), "1.0"));

    persister.persist();

    checkTables("shouldSaveNewMultiModulesProjectAndLibrary",
      new String[] {"build_date", "created_at", "authorization_updated_at", "uuid", "project_uuid", "module_uuid", "module_uuid_path"}, "projects", "snapshots");

    // Need to enable snapshot to make resource visible using ComponentMapper
    enableSnapshot(1001);
    enableSnapshot(1002);
    enableSnapshot(1003);
    enableSnapshot(1004);
    enableSnapshot(1005);
    enableSnapshot(1006);
    try (SqlSession session = getMyBatis().openSession(false)) {
      ComponentDto root = session.getMapper(ComponentMapper.class).selectByKey("root");
      System.out.println("Root: " + root.uuid());
      assertThat(root.uuid()).isNotNull();
      assertThat(root.projectUuid()).isEqualTo(root.uuid());
      assertThat(root.moduleUuid()).isNull();
      assertThat(root.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto a = session.getMapper(ComponentMapper.class).selectByKey("a");
      System.out.println("A: " + a.uuid());
      assertThat(a.uuid()).isNotNull();
      assertThat(a.projectUuid()).isEqualTo(root.uuid());
      assertThat(a.moduleUuid()).isEqualTo(root.uuid());
      assertThat(a.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + a.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto b = session.getMapper(ComponentMapper.class).selectByKey("b");
      System.out.println("B: " + b.uuid());
      assertThat(b.uuid()).isNotNull();
      assertThat(b.projectUuid()).isEqualTo(root.uuid());
      assertThat(b.moduleUuid()).isEqualTo(root.uuid());
      assertThat(b.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto b1 = session.getMapper(ComponentMapper.class).selectByKey("b1");
      System.out.println("B1: " + b1.uuid());
      assertThat(b1.uuid()).isNotNull();
      assertThat(b1.projectUuid()).isEqualTo(root.uuid());
      assertThat(b1.moduleUuid()).isEqualTo(b.uuid());
      assertThat(b1.moduleUuidPath()).isEqualTo(
        MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR + b1.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto dir = session.getMapper(ComponentMapper.class).selectByKey("b1:src/main/java/org");
      assertThat(dir.uuid()).isNotNull();
      assertThat(dir.projectUuid()).isEqualTo(root.uuid());
      assertThat(dir.moduleUuid()).isEqualTo(b1.uuid());
      assertThat(dir.moduleUuidPath()).isEqualTo(
        MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR + b1.uuid() + MODULE_UUID_PATH_SEPARATOR);
      ComponentDto fileComp = session.getMapper(ComponentMapper.class).selectByKey("b1:src/main/java/org/Foo.java");
      assertThat(fileComp.uuid()).isNotNull();
      assertThat(fileComp.projectUuid()).isEqualTo(root.uuid());
      assertThat(fileComp.moduleUuid()).isEqualTo(b1.uuid());
      assertThat(fileComp.moduleUuidPath()).isEqualTo(
        MODULE_UUID_PATH_SEPARATOR + root.uuid() + MODULE_UUID_PATH_SEPARATOR + b.uuid() + MODULE_UUID_PATH_SEPARATOR + b1.uuid() + MODULE_UUID_PATH_SEPARATOR);
    }
  }

  @Test
  public void shouldSaveNewDirectory() {
    setupData("shared");

    resourceCache.add(singleProject, null).setSnapshot(persister.persist(null, singleProject, null));
    persister.persist(singleProject, Directory.create("src/main/java/org/foo").setEffectiveKey("foo:src/main/java/org/foo"), null);
    // check that the directory is attached to the project
    checkTables("shouldSaveNewDirectory", new String[] {"build_date", "created_at", "authorization_updated_at", "uuid", "project_uuid", "module_uuid", "module_uuid_path"},
      "projects", "snapshots");

    // Need to enable snapshot to make resource visible using ComponentMapper
    enableSnapshot(1001);
    enableSnapshot(1002);
    try (SqlSession session = getMyBatis().openSession(false)) {
      ComponentDto newProject = session.getMapper(ComponentMapper.class).selectByKey("foo");
      ComponentDto newDir = session.getMapper(ComponentMapper.class).selectByKey("foo:src/main/java/org/foo");
      assertThat(newDir.uuid()).isNotNull();
      assertThat(newDir.projectUuid()).isEqualTo(newProject.uuid());
      assertThat(newDir.moduleUuid()).isEqualTo(newProject.uuid());
      assertThat(newDir.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + newProject.uuid() + MODULE_UUID_PATH_SEPARATOR);
    }
  }

  private void enableSnapshot(int resourceId) {
    String hql = "UPDATE " + Snapshot.class.getSimpleName() + " SET last=true";
    hql += " WHERE project_id=:resourceId";
    Query query = getSession().createQuery(hql);
    query.setParameter("resourceId", resourceId);
    query.executeUpdate();
    getSession().commit();
  }

  @Test
  public void shouldSaveNewLibrary() {
    setupData("shared");

    persister.persist(null, singleProject, null);
    persister.persistLibrary(singleProject.getAnalysisDate(), (Library) new Library("junit:junit", "4.8.2").setEffectiveKey("junit:junit"));
    persister.persistLibrary(singleProject.getAnalysisDate(), (Library) new Library("junit:junit", "4.8.2").setEffectiveKey("junit:junit"));// do
    // nothing,
    // already
    // saved
    persister.persistLibrary(singleProject.getAnalysisDate(), (Library) new Library("junit:junit", "3.2").setEffectiveKey("junit:junit"));

    checkTables("shouldSaveNewLibrary", new String[] {"build_date", "created_at", "authorization_updated_at", "uuid", "project_uuid", "module_uuid", "module_uuid_path"},
      "projects", "snapshots");

    // Need to enable snapshot to make resource visible using ComponentMapper
    enableSnapshot(1002);
    try (SqlSession session = getMyBatis().openSession(false)) {
      // FIXME selectByKey returns duplicates for libraries because of the join on snapshots table
      ComponentDto newLib = session.getMapper(ComponentMapper.class).findByKeys(Arrays.asList("junit:junit")).get(0);
      assertThat(newLib.uuid()).isNotNull();
      assertThat(newLib.projectUuid()).isEqualTo(newLib.uuid());
      assertThat(newLib.moduleUuid()).isNull();
      assertThat(newLib.moduleUuidPath()).isEqualTo(MODULE_UUID_PATH_SEPARATOR + newLib.uuid() + MODULE_UUID_PATH_SEPARATOR);
    }
  }

  @Test
  public void shouldUpdateExistingResource() {
    setupData("shouldUpdateExistingResource");

    singleProject.setName("new name");
    singleProject.setDescription("new description");
    persister.persist(null, singleProject, null);

    checkTables("shouldUpdateExistingResource", new String[] {"build_date", "created_at", "authorization_updated_at"}, "projects", "snapshots");
  }

  // SONAR-1700
  @Test
  public void shouldRemoveRootIndexIfResourceIsProject() {
    setupData("shouldRemoveRootIndexIfResourceIsProject");

    persister.persist(null, singleProject, null);

    checkTables("shouldRemoveRootIndexIfResourceIsProject", new String[] {"build_date", "created_at", "authorization_updated_at"}, "projects", "snapshots");
  }

  @Test
  public void shouldGrantDefaultPermissionsIfNewProject() {
    setupData("shared");

    when(permissions.hasRoles(singleProject)).thenReturn(false);

    persister.persist(null, singleProject, null);

    verify(permissions).grantDefaultRoles(singleProject);
  }

  @Test
  public void shouldNotGrantDefaultPermissionsOnModules() {
    setupData("shared");
    resourceCache.add(multiModuleProject, null).setSnapshot(persister.persist(null, multiModuleProject, null));
    resourceCache.add(moduleA, multiModuleProject).setSnapshot(persister.persist(null, moduleA, multiModuleProject));
    when(permissions.hasRoles(multiModuleProject)).thenReturn(true);
    persister.persist(null, multiModuleProject, null);

    persister.persist(null, moduleA, multiModuleProject);

    verify(permissions, never()).grantDefaultRoles(moduleA);
  }

  @Test
  public void shouldNotGrantDefaultPermissionsIfExistingProject() {
    setupData("shared");

    when(permissions.hasRoles(singleProject)).thenReturn(true);

    persister.persist(null, singleProject, null);

    verify(permissions, never()).grantDefaultRoles(singleProject);
  }

  private static Project newProject(String key, String language) {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, language);
    return new Project(key).setSettings(settings).setAnalysisType(Project.AnalysisType.DYNAMIC);
  }

  private static Project newCopyProject(String key, String language, int copyResourceId) {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, language);
    return new CopyProject(key, copyResourceId).setSettings(settings).setAnalysisType(Project.AnalysisType.DYNAMIC);
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
