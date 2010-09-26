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
package org.sonar.batch;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.resources.Project;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;


public class ProjectTreeTest extends AbstractDbUnitTestCase {

  @Test
  public void moduleNameShouldEqualArtifactId() throws Exception {
    MavenProject parent = loadProject("/org/sonar/batch/ProjectTreeTest/moduleNameShouldEqualArtifactId/pom.xml", true);
    MavenProject module1 = loadProject("/org/sonar/batch/ProjectTreeTest/moduleNameShouldEqualArtifactId/module1/pom.xml", false);
    MavenProject module2 = loadProject("/org/sonar/batch/ProjectTreeTest/moduleNameShouldEqualArtifactId/module2/pom.xml", false);

    ProjectTree tree = new ProjectTree(newProjectBuilder(), Arrays.asList(parent, module1, module2));
    tree.start();

    Project root = tree.getRootProject();
    assertThat(root.getModules().size(), is(2));
    assertThat(root.getKey(), is("org.test:parent"));
    assertNull(root.getParent());
    assertThat(tree.getProjectByArtifactId("module1").getKey(), is("org.test:module1"));
    assertThat(tree.getProjectByArtifactId("module1").getParent(), is(root));
    assertThat(tree.getProjectByArtifactId("module2").getKey(), is("org.test:module2"));
    assertThat(tree.getProjectByArtifactId("module2").getParent(), is(root));
  }

  @Test
  public void moduleNameDifferentThanArtifactId() throws Exception {
    MavenProject parent = loadProject("/org/sonar/batch/ProjectTreeTest/moduleNameDifferentThanArtifactId/pom.xml", true);
    MavenProject module1 = loadProject("/org/sonar/batch/ProjectTreeTest/moduleNameDifferentThanArtifactId/path1/pom.xml", false);
    MavenProject module2 = loadProject("/org/sonar/batch/ProjectTreeTest/moduleNameDifferentThanArtifactId/path2/pom.xml", false);

    ProjectTree tree = new ProjectTree(newProjectBuilder(), Arrays.asList(parent, module1, module2));
    tree.start();


    Project root = tree.getRootProject();
    assertThat(root.getModules().size(), is(2));
    assertThat(root.getKey(), is("org.test:parent"));
    assertNull(root.getParent());
    assertThat(tree.getProjectByArtifactId("module1").getKey(), is("org.test:module1"));
    assertThat(tree.getProjectByArtifactId("module1").getParent(), is(root));
    assertThat(tree.getProjectByArtifactId("module2").getKey(), is("org.test:module2"));
    assertThat(tree.getProjectByArtifactId("module2").getParent(), is(root));
  }

  @Test
  public void singleProjectWithoutModules() throws Exception {
    MavenProject parent = loadProject("/org/sonar/batch/ProjectTreeTest/singleProjectWithoutModules/pom.xml", true);

    ProjectTree tree = new ProjectTree(newProjectBuilder(), Arrays.asList(parent));
    tree.start();

    Project root = tree.getRootProject();
    assertThat(root.getModules().size(), is(0));
    assertThat(root.getKey(), is("org.test:parent"));
  }

  @Test
  public void keyIncludesBranch() throws IOException, XmlPullParserException, URISyntaxException {
    MavenProject pom = loadProject("/org/sonar/batch/ProjectTreeTest/keyIncludesBranch/pom.xml", true);

    ProjectTree tree = new ProjectTree(newProjectBuilder(), Arrays.asList(pom));
    tree.start();

    assertThat(tree.getRootProject().getKey(), is("org.test:project:BRANCH-1.X"));
    assertThat(tree.getRootProject().getName(), is("Project BRANCH-1.X"));
  }


  @Test
  public void keyIncludesDeprecatedBranch() throws IOException, XmlPullParserException, URISyntaxException {
    MavenProject pom = loadProject("/org/sonar/batch/ProjectTreeTest/keyIncludesDeprecatedBranch/pom.xml", true);

    ProjectTree tree = new ProjectTree(newProjectBuilder(), Arrays.asList(pom));
    tree.start();

    assertThat(tree.getRootProject().getKey(), is("org.test:project:BRANCH-1.X"));
    assertThat(tree.getRootProject().getName(), is("Project BRANCH-1.X"));
  }

  @Test
  public void doNotSkipAnyModules() {
    Project foo = newProjectWithArtifactId("root");
    Project bar = newProjectWithArtifactId("sub1");
    Project baz = newProjectWithArtifactId("sub2");

    ProjectTree tree = new ProjectTree(Arrays.asList(foo, bar, baz));
    tree.applyModuleExclusions();

    assertThat(tree.getProjects().size(), is(3));
  }

  @Test
  public void skipModule() {
    Project root = newProjectWithArtifactId("root");
    root.getConfiguration().setProperty("sonar.skippedModules", "sub1");
    Project sub1 = newProjectWithArtifactId("sub1");
    Project sub2 = newProjectWithArtifactId("sub2");

    ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
    tree.applyModuleExclusions();

    assertThat(tree.getProjects().size(), is(2));
    assertThat(tree.getProjects(), hasItem(root));
    assertThat(tree.getProjects(), hasItem(sub2));
  }

  @Test
  public void skipModules() {
    Project root = newProjectWithArtifactId("root");
    root.getConfiguration().setProperty("sonar.skippedModules", "sub1,sub2");
    Project sub1 = newProjectWithArtifactId("sub1");
    Project sub2 = newProjectWithArtifactId("sub2");

    ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
    tree.applyModuleExclusions();

    assertThat(tree.getProjects().size(), is(1));
    assertThat(tree.getProjects(), hasItem(root));
  }

  @Test
  public void includeModules() {
    Project root = newProjectWithArtifactId("root");
    root.getConfiguration().setProperty("sonar.includedModules", "sub1,sub2");
    Project sub1 = newProjectWithArtifactId("sub1");
    Project sub2 = newProjectWithArtifactId("sub2");

    ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
    tree.applyModuleExclusions();

    assertThat(tree.getProjects().size(), is(2));
    assertThat(tree.getProjects(), hasItem(sub1));
    assertThat(tree.getProjects(), hasItem(sub2));
  }

  @Test
  public void skippedModulesTakePrecedenceOverIncludedModules() {
    Project root = newProjectWithArtifactId("root");
    root.getConfiguration().setProperty("sonar.includedModules", "sub1,sub2");
    root.getConfiguration().setProperty("sonar.skippedModules", "sub1");
    Project sub1 = newProjectWithArtifactId("sub1");
    Project sub2 = newProjectWithArtifactId("sub2");

    ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
    tree.applyModuleExclusions();

    assertThat(tree.getProjects().size(), is(1));
    assertThat(tree.getProjects(), hasItem(sub2));
  }

  private Project newProjectWithArtifactId(String artifactId) {
    MavenProject pom = new MavenProject();
    pom.setArtifactId(artifactId);
    return new Project(artifactId).setPom(pom).setConfiguration(new PropertiesConfiguration());
  }

  private MavenProject loadProject(String pomPath, boolean isRoot) throws URISyntaxException, IOException, XmlPullParserException {
    File pomFile = new File(getClass().getResource(pomPath).toURI());
    Model model = new MavenXpp3Reader().read(new StringReader(FileUtils.readFileToString(pomFile)));
    MavenProject pom = new MavenProject(model);
    pom.setFile(pomFile);
    pom.setExecutionRoot(isRoot);
    return pom;
  }

  private MavenProjectBuilder newProjectBuilder() {
    return new MavenProjectBuilder(getSession());
  }
}
