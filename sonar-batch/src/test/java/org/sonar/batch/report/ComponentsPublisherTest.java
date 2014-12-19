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
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourceCache;

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
    resourceCache.add(root, null, new Snapshot().setId(11));
    Project module1 = new Project("module1").setName("Module1");
    module1.setParent(root);
    module1.setId(2);
    resourceCache.add(module1, root, new Snapshot().setId(12));
    Directory dir1 = Directory.create("src");
    dir1.setEffectiveKey("foo:src");
    dir1.setId(3);
    resourceCache.add(dir1, module1, new Snapshot().setId(13));
    org.sonar.api.resources.File mainFile = org.sonar.api.resources.File.create("src/Foo.java", "Foo.java", Java.INSTANCE, false);
    mainFile.setEffectiveKey("foo:src/Foo.java");
    mainFile.setId(4);
    resourceCache.add(mainFile, dir1, new Snapshot().setId(14));
    Directory dir2 = Directory.create("test");
    dir2.setEffectiveKey("foo:test");
    dir2.setId(5);
    resourceCache.add(dir2, module1, new Snapshot().setId(15));
    org.sonar.api.resources.File testFile = org.sonar.api.resources.File.create("test/FooTest.java", "FooTest.java", Java.INSTANCE, true);
    testFile.setEffectiveKey("foo:test/FooTest.java");
    testFile.setId(6);
    resourceCache.add(testFile, dir2, new Snapshot().setId(16));

    File exportDir = temp.newFolder();
    publisher.export(exportDir);

    System.out.println(FileUtils.readFileToString(new File(exportDir, "components.json")));

    JSONAssert
      .assertEquals(
        IOUtils.toString(this.getClass().getResourceAsStream("ComponentsPublisherTest/expected.json"), "UTF-8"),
        FileUtils.readFileToString(new File(exportDir, "components.json")), true);
  }
}
