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
package org.sonar.batch;

import org.junit.Ignore;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

@Ignore
public class ProjectTreeTest extends AbstractDbUnitTestCase {

  // @Test
  // public void keyIncludesBranch() throws IOException, XmlPullParserException, URISyntaxException {
  // MavenProject pom = loadProject("/org/sonar/batch/ProjectTreeTest/keyIncludesBranch/pom.xml", true);
  //
  // ProjectTree tree = new ProjectTree(newConfigurator(), Arrays.asList(pom));
  // tree.start();
  //
  // assertThat(tree.getRootProject().getKey(), is("org.test:project:BRANCH-1.X"));
  // assertThat(tree.getRootProject().getName(), is("Project BRANCH-1.X"));
  // }
  //
  // @Test
  // public void doNotSkipAnyModules() {
  // Project foo = newProjectWithArtifactId("root");
  // Project bar = newProjectWithArtifactId("sub1");
  // Project baz = newProjectWithArtifactId("sub2");
  //
  // ProjectTree tree = new ProjectTree(Arrays.asList(foo, bar, baz));
  // tree.applyExclusions();
  //
  // assertThat(tree.getProjects().size(), is(3));
  // }
  //
  // @Test
  // public void skipModule() throws IOException {
  // Project root = newProjectWithArtifactId("root");
  // root.getConfiguration().setProperty("sonar.skippedModules", "sub1");
  // Project sub1 = newProjectWithArtifactId("sub1");
  // Project sub2 = newProjectWithArtifactId("sub2");
  //
  // ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
  // tree.applyExclusions();
  //
  // assertThat(tree.getProjects().size(), is(2));
  // assertThat(tree.getProjects(), hasItem(root));
  // assertThat(tree.getProjects(), hasItem(sub2));
  // }
  //
  // @Test
  // public void skipModules() {
  // Project root = newProjectWithArtifactId("root");
  // root.getConfiguration().setProperty("sonar.skippedModules", "sub1,sub2");
  // Project sub1 = newProjectWithArtifactId("sub1");
  // Project sub2 = newProjectWithArtifactId("sub2");
  //
  // ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
  // tree.applyExclusions();
  //
  // assertThat(tree.getProjects().size(), is(1));
  // assertThat(tree.getProjects(), hasItem(root));
  // }
  //
  // @Test
  // public void includeModules() {
  // Project root = newProjectWithArtifactId("root");
  // root.getConfiguration().setProperty("sonar.includedModules", "sub1,sub2");
  // Project sub1 = newProjectWithArtifactId("sub1");
  // Project sub2 = newProjectWithArtifactId("sub2");
  //
  // ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
  // tree.applyExclusions();
  //
  // assertThat(tree.getProjects().size(), is(2));
  // assertThat(tree.getProjects(), hasItem(sub1));
  // assertThat(tree.getProjects(), hasItem(sub2));
  // }
  //
  // @Test
  // public void skippedModulesTakePrecedenceOverIncludedModules() {
  // Project root = newProjectWithArtifactId("root");
  // root.getConfiguration().setProperty("sonar.includedModules", "sub1,sub2");
  // root.getConfiguration().setProperty("sonar.skippedModules", "sub1");
  // Project sub1 = newProjectWithArtifactId("sub1");
  // Project sub2 = newProjectWithArtifactId("sub2");
  //
  // ProjectTree tree = new ProjectTree(Arrays.asList(root, sub1, sub2));
  // tree.applyExclusions();
  //
  // assertThat(tree.getProjects().size(), is(1));
  // assertThat(tree.getProjects(), hasItem(sub2));
  // }

  // private Project newProjectWithArtifactId(String artifactId) {
  // MavenProject pom = new MavenProject();
  // pom.setArtifactId(artifactId);
  // return new Project("org.example:" + artifactId).setPom(pom).setConfiguration(new PropertiesConfiguration());
  // }

}
