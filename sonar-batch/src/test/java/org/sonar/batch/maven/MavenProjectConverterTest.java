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
package org.sonar.batch.maven;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MavenProjectConverterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  MavenProjectConverter converter = new MavenProjectConverter();

  /**
   * See SONAR-2681
   */
  @Test
  public void shouldThrowExceptionWhenUnableToDetermineProjectStructure() {
    MavenProject root = new MavenProject();
    root.setFile(new File("/foo/pom.xml"));
    root.getBuild().setDirectory("target");
    root.getModules().add("module/pom.xml");

    try {
      converter.configure(Arrays.asList(root), root);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), containsString("Advanced Reactor Options"));
    }
  }

  @Test
  public void shouldConvertModules() throws IOException {
    File basedir = temp.newFolder();

    MavenProject root = newMavenProject("com.foo", "parent", "1.0-SNAPSHOT");
    root.setFile(new File(basedir, "pom.xml"));
    root.getBuild().setDirectory("target");
    root.getBuild().setOutputDirectory("target/classes");
    root.getModules().add("module/pom.xml");
    MavenProject module = newMavenProject("com.foo", "moduleA", "1.0-SNAPSHOT");
    module.setFile(new File(basedir, "module/pom.xml"));
    module.getBuild().setDirectory("target");
    module.getBuild().setOutputDirectory("target/classes");
    ProjectDefinition project = converter.configure(Arrays.asList(root, module), root);

    assertThat(project.getSubProjects().size(), is(1));
  }

  private MavenProject newMavenProject(String groupId, String artifactId, String version) {
    Model model = new Model();
    model.setGroupId(groupId);
    model.setArtifactId(artifactId);
    model.setVersion(version);
    return new MavenProject(model);
  }

  @Test
  public void shouldConvertProperties() {
    MavenProject pom = new MavenProject();
    pom.setGroupId("foo");
    pom.setArtifactId("bar");
    pom.setVersion("1.0.1");
    pom.setName("Test");
    pom.setDescription("just test");
    pom.setFile(new File("/foo/pom.xml"));
    pom.getBuild().setDirectory("target");
    ProjectDefinition project = ProjectDefinition.create();
    converter.merge(pom, project);

    Properties properties = project.getProperties();
    assertThat(properties.getProperty(CoreProperties.PROJECT_KEY_PROPERTY), is("foo:bar"));
    assertThat(properties.getProperty(CoreProperties.PROJECT_VERSION_PROPERTY), is("1.0.1"));
    assertThat(properties.getProperty(CoreProperties.PROJECT_NAME_PROPERTY), is("Test"));
    assertThat(properties.getProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY), is("just test"));
  }

  @Test
  public void moduleNameShouldEqualArtifactId() throws Exception {
    File rootDir = new File(Resources.getResource("org/sonar/batch/maven/MavenProjectConverterTest/moduleNameShouldEqualArtifactId/").toURI());
    MavenProject parent = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/moduleNameShouldEqualArtifactId/pom.xml", true);
    MavenProject module1 = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/moduleNameShouldEqualArtifactId/module1/pom.xml", false);
    MavenProject module2 = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/moduleNameShouldEqualArtifactId/module2/pom.xml", false);

    ProjectDefinition rootDef = converter.configure(Arrays.asList(parent, module1, module2), parent);

    assertThat(rootDef.getSubProjects().size(), Is.is(2));
    assertThat(rootDef.getKey(), Is.is("org.test:parent"));
    assertNull(rootDef.getParent());
    assertThat(rootDef.getBaseDir(), is(rootDir));

    ProjectDefinition module1Def = rootDef.getSubProjects().get(0);
    assertThat(module1Def.getKey(), Is.is("org.test:module1"));
    assertThat(module1Def.getParent(), Is.is(rootDef));
    assertThat(module1Def.getBaseDir(), Is.is(new File(rootDir, "module1")));
    assertThat(module1Def.getSubProjects().size(), Is.is(0));
  }

  @Test
  public void moduleNameDifferentThanArtifactId() throws Exception {
    File rootDir = new File(Resources.getResource("org/sonar/batch/maven/MavenProjectConverterTest/moduleNameDifferentThanArtifactId/").toURI());
    MavenProject parent = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/moduleNameDifferentThanArtifactId/pom.xml", true);
    MavenProject module1 = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/moduleNameDifferentThanArtifactId/path1/pom.xml", false);
    MavenProject module2 = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/moduleNameDifferentThanArtifactId/path2/pom.xml", false);

    ProjectDefinition rootDef = converter.configure(Arrays.asList(parent, module1, module2), parent);

    assertThat(rootDef.getSubProjects().size(), is(2));
    assertThat(rootDef.getKey(), is("org.test:parent"));
    assertNull(rootDef.getParent());
    assertThat(rootDef.getBaseDir(), is(rootDir));

    ProjectDefinition module1Def = rootDef.getSubProjects().get(0);
    assertThat(module1Def.getKey(), Is.is("org.test:module1"));
    assertThat(module1Def.getParent(), Is.is(rootDef));
    assertThat(module1Def.getBaseDir(), Is.is(new File(rootDir, "path1")));
    assertThat(module1Def.getSubProjects().size(), Is.is(0));
  }

  @Test
  public void should_find_module_with_maven_project_file_naming_different_from_pom_xml() throws Exception {
    File rootDir = new File(Resources.getResource("org/sonar/batch/maven/MavenProjectConverterTest/mavenProjectFileNameNotEqualsToPomXml/").toURI());
    MavenProject parent = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/mavenProjectFileNameNotEqualsToPomXml/pom.xml", true);
    MavenProject module = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/mavenProjectFileNameNotEqualsToPomXml/module/pom_having_different_name.xml", false);

    ProjectDefinition rootDef = converter.configure(Arrays.asList(parent, module), parent);

    assertThat(rootDef.getSubProjects().size(), Is.is(1));
    assertThat(rootDef.getKey(), Is.is("org.test:parent"));
    assertNull(rootDef.getParent());
    assertThat(rootDef.getBaseDir(), is(rootDir));

    ProjectDefinition module1Def = rootDef.getSubProjects().get(0);
    assertThat(module1Def.getKey(), Is.is("org.test:module"));
    assertThat(module1Def.getParent(), Is.is(rootDef));
    assertThat(module1Def.getBaseDir(), Is.is(new File(rootDir, "module")));
    assertThat(module1Def.getSubProjects().size(), Is.is(0));
  }

  @Test
  public void testSingleProjectWithoutModules() throws Exception {
    File rootDir = new File(Resources.getResource("org/sonar/batch/maven/MavenProjectConverterTest/singleProjectWithoutModules/").toURI());
    MavenProject pom = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/singleProjectWithoutModules/pom.xml", true);

    ProjectDefinition rootDef = converter.configure(Arrays.asList(pom), pom);

    assertThat(rootDef.getKey(), is("org.test:parent"));
    assertThat(rootDef.getSubProjects().size(), is(0));
    assertNull(rootDef.getParent());
    assertThat(rootDef.getBaseDir(), is(rootDir));
  }

  @Test
  public void shouldConvertLinksToProperties() throws Exception {
    MavenProject pom = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/projectWithLinks/pom.xml", true);

    ProjectDefinition rootDef = converter.configure(Arrays.asList(pom), pom);

    Properties props = rootDef.getProperties();
    assertThat(props.getProperty(CoreProperties.LINKS_HOME_PAGE)).isEqualTo("http://home.com");
    assertThat(props.getProperty(CoreProperties.LINKS_CI)).isEqualTo("http://ci.com");
    assertThat(props.getProperty(CoreProperties.LINKS_ISSUE_TRACKER)).isEqualTo("http://issues.com");
    assertThat(props.getProperty(CoreProperties.LINKS_SOURCES)).isEqualTo("http://sources.com");
    assertThat(props.getProperty(CoreProperties.LINKS_SOURCES_DEV)).isEqualTo("http://sources-dev.com");
  }

  @Test
  public void shouldNotConvertLinksToPropertiesIfPropertyAlreadyDefined() throws Exception {
    MavenProject pom = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/projectWithLinksAndProperties/pom.xml", true);

    ProjectDefinition rootDef = converter.configure(Arrays.asList(pom), pom);

    Properties props = rootDef.getProperties();

    // Those properties have been fed by the POM elements <ciManagement>, <issueManagement>, ...
    assertThat(props.getProperty(CoreProperties.LINKS_CI)).isEqualTo("http://ci.com");
    assertThat(props.getProperty(CoreProperties.LINKS_ISSUE_TRACKER)).isEqualTo("http://issues.com");
    assertThat(props.getProperty(CoreProperties.LINKS_SOURCES_DEV)).isEqualTo("http://sources-dev.com");

    // ... but those ones have been overridden by <properties> in the POM
    assertThat(props.getProperty(CoreProperties.LINKS_SOURCES)).isEqualTo("http://sources.com-OVERRIDEN-BY-PROPS");
    assertThat(props.getProperty(CoreProperties.LINKS_HOME_PAGE)).isEqualTo("http://home.com-OVERRIDEN-BY-PROPS");
  }

  @Test
  public void shouldLoadSourceEncoding() throws Exception {
    MavenProject pom = loadPom("/org/sonar/batch/maven/MavenProjectConverterTest/sourceEncoding/pom.xml", true);

    ProjectDefinition rootDef = converter.configure(Arrays.asList(pom), pom);

    assertThat(rootDef.getProperties().getProperty(CoreProperties.ENCODING_PROPERTY)).isEqualTo("Shift_JIS");
  }

  private MavenProject loadPom(String pomPath, boolean isRoot) throws URISyntaxException, IOException, XmlPullParserException {
    File pomFile = new File(getClass().getResource(pomPath).toURI());
    Model model = new MavenXpp3Reader().read(new StringReader(FileUtils.readFileToString(pomFile)));
    MavenProject pom = new MavenProject(model);
    pom.setFile(pomFile);
    pom.getBuild().setDirectory("target");
    pom.setExecutionRoot(isRoot);
    return pom;
  }
}
