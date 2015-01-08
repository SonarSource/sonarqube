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
package org.sonar.batch.report;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.ReportHelper;

import java.io.File;
import java.text.SimpleDateFormat;

public class ComponentsPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testComponentPublisher() throws Exception {
    ProjectReactor reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo"));
    ResourceCache resourceCache = new ResourceCache();
    ComponentsPublisher publisher = new ComponentsPublisher(reactor, resourceCache);

    Project root = new Project("foo").setName("Root project").setAnalysisDate(new SimpleDateFormat("dd/MM/yyyy").parse("12/12/2012"));
    root.setId(1);
    resourceCache.add(root, null).setSnapshot(new Snapshot().setId(11));
    Project module1 = new Project("module1").setName("Module1");
    module1.setParent(root);
    module1.setId(2);
    resourceCache.add(module1, root).setSnapshot(new Snapshot().setId(12));
    Directory dir1 = Directory.create("src");
    dir1.setEffectiveKey("foo:src");
    dir1.setId(3);
    resourceCache.add(dir1, module1).setSnapshot(new Snapshot().setId(13));
    org.sonar.api.resources.File mainFile = org.sonar.api.resources.File.create("src/Foo.java", "Foo.java", Java.INSTANCE, false);
    mainFile.setEffectiveKey("foo:src/Foo.java");
    mainFile.setId(4);
    resourceCache.add(mainFile, dir1).setSnapshot(new Snapshot().setId(14));
    Directory dir2 = Directory.create("test");
    dir2.setEffectiveKey("foo:test");
    dir2.setId(5);
    resourceCache.add(dir2, module1).setSnapshot(new Snapshot().setId(15));
    org.sonar.api.resources.File testFile = org.sonar.api.resources.File.create("test/FooTest.java", "FooTest.java", Java.INSTANCE, true);
    testFile.setEffectiveKey("foo:test/FooTest.java");
    testFile.setId(6);
    resourceCache.add(testFile, dir2).setSnapshot(new Snapshot().setId(16));

    File exportDir = temp.newFolder();
    ReportHelper helper = ReportHelper.create(exportDir);
    publisher.export(helper);

    JSONAssert
      .assertEquals(
        IOUtils.toString(this.getClass().getResourceAsStream("ComponentsPublisherTest/expected.json"), "UTF-8"),
        FileUtils.readFileToString(new File(exportDir, "components.json")), true);
  }

  @Test
  public void testComponentPublisher_containing_file_without_language() throws Exception {
    ProjectReactor reactor = new ProjectReactor(ProjectDefinition.create().setKey("ALL_PROJECT"));
    ResourceCache resourceCache = new ResourceCache();
    ComponentsPublisher publisher = new ComponentsPublisher(reactor, resourceCache);

    View view = new View("ALL_PROJECT");
    view.setId(1);
    view.setAnalysisDate(new SimpleDateFormat("dd/MM/yyyy").parse("12/12/2012"));
    resourceCache.add(view, null).setSnapshot(new Snapshot().setId(11));

    org.sonar.api.resources.File mainFile = org.sonar.api.resources.File.create("ALL_PROJECTsample", "ALL_PROJECTsample", null, false);
    mainFile.setEffectiveKey("ALL_PROJECTsample");
    mainFile.setId(2);
    resourceCache.add(mainFile, view).setSnapshot(new Snapshot().setId(12));

    File exportDir = temp.newFolder();
    ReportHelper helper = ReportHelper.create(exportDir);
    publisher.export(helper);

    JSONAssert
      .assertEquals(
        IOUtils.toString(this.getClass().getResourceAsStream("ComponentsPublisherTest/testComponentPublisher_containing_file_without_language.json"), "UTF-8"),
        FileUtils.readFileToString(new File(exportDir, "components.json")), true);
  }

  private static class View extends Project {

    private View(String key) {
      super(key);
    }

    @Override
    public String getName() {
      return "All Projects";
    }

    @Override
    public String getLongName() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public Language getLanguage() {
      return null;
    }

    @Override
    public String getScope() {
      return Scopes.PROJECT;
    }

    @Override
    public String getQualifier() {
      return Qualifiers.VIEW;
    }

    @Override
    public Project getParent() {
      return null;
    }

    @Override
    public boolean matchFilePattern(String antPattern) {
      return false;
    }
  }

}
