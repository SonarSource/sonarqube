/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.ScannerProperties;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ProjectReactorBuilderTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void projectBaseDirDefaultToCurrentDirectory() {
    ScannerProperties bootstrapProps = new ScannerProperties(Map.of("sonar.projectKey", "foo"));
    ProjectReactor projectReactor = new ProjectReactorBuilder(bootstrapProps, mock(AnalysisWarnings.class)).execute();
    var def = projectReactor.getRoot();
    assertThat(def.getBaseDir()).isEqualTo(new File("").getAbsoluteFile());
  }

  @Test
  public void shouldDefineSimpleProject() {
    ProjectDefinition projectDefinition = loadProjectDefinition("simple-project");

    assertThat(projectDefinition.getKey()).isEqualTo("com.foo.project");
    assertThat(projectDefinition.getName()).isEqualTo("Foo Project");
    assertThat(projectDefinition.getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(projectDefinition.getDescription()).isEqualTo("Description of Foo Project");
    assertThat(projectDefinition.sources()).contains("sources");
  }

  @Test
  public void should_fail_if_sources_are_missing_in_leaf_module() {
    assertThatThrownBy(() -> loadProjectDefinition("simple-project-with-unexisting-source-dir"))
      .isInstanceOf(MessageException.class)
      .hasMessage("The folder 'unexisting-source-dir' does not exist for 'com.foo.project' (base directory = "
        + getResource(this.getClass(), "simple-project-with-unexisting-source-dir") + ")");
  }

  @Test
  public void should_not_fail_if_sources_are_missing_in_intermediate_module() {
    assertThatNoException()
      .isThrownBy(() -> loadProjectDefinition("multi-module-pom-in-root"));
  }

  @Test
  public void shouldNotFailIfBlankSourceDirectory() {
    assertThatNoException()
      .isThrownBy(() -> loadProjectDefinition("simple-project-with-blank-source-dir"));
  }

  @Test
  public void modulesDuplicateIds() {
    assertThatThrownBy(() -> loadProjectDefinition("multi-module-duplicate-id"))
      .isInstanceOf(MessageException.class)
      .hasMessage("Two modules have the same id: 'module1'. Each module must have a unique id.");
  }

  @Test
  public void sonarModuleIdIsForbidden() {
    assertThatThrownBy(() -> loadProjectDefinition("multi-module-sonar-module"))
      .isInstanceOf(MessageException.class)
      .hasMessage("'sonar' is not a valid module id. Please check property 'sonar.modules'.");
  }

  @Test
  public void modulesRepeatedIds() {
    ProjectDefinition rootProject = loadProjectDefinition("multi-module-repeated-id");

    List<ProjectDefinition> modules = rootProject.getSubProjects();
    assertThat(modules.size()).isOne();
    // Module 1
    ProjectDefinition module1 = modules.get(0);
    assertThat(module1.getKey()).isEqualTo("com.foo.project:module1");
    assertThat(module1.getName()).isEqualTo("Foo Module 1");

    // Module 1 -> Module 1
    ProjectDefinition module1_module1 = module1.getSubProjects().get(0);
    assertThat(module1_module1.getKey()).isEqualTo("com.foo.project:module1:module1");
    assertThat(module1_module1.getName()).isEqualTo("Foo Sub Module 1");
  }

  @Test
  public void shouldDefineMultiModuleProjectWithDefinitionsAllInRootProject() throws IOException {
    execMultiModule("multi-module-definitions-all-in-root");
  }

  @Test
  public void shouldDefineMultiModuleProjectWithPomFileAtRootLevel() throws IOException {
    ProjectDefinition project = execMultiModule("multi-module-pom-in-root");
    assertThat(project.sources()).containsExactlyInAnyOrder("pom.xml", "sources");
  }

  public ProjectDefinition execMultiModule(String key) throws IOException {
    ProjectDefinition rootProject = loadProjectDefinition(key);
    // CHECK ROOT
    assertThat(rootProject.getKey()).isEqualTo("com.foo.project");
    assertThat(rootProject.getName()).isEqualTo("Foo Project");
    assertThat(rootProject.getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(rootProject.getDescription()).isEqualTo("Description of Foo Project");
    assertThat(rootProject.sources()).contains("sources");
    assertThat(rootProject.tests()).contains("tests");
    // and module properties must have been cleaned
    assertThat(rootProject.properties().get("module1.sonar.projectKey")).isNull();
    assertThat(rootProject.properties().get("module2.sonar.projectKey")).isNull();
    // Check baseDir and workDir
    assertThat(rootProject.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), key));
    assertThat(rootProject.getWorkDir().getCanonicalFile()).isEqualTo(new File(getResource(this.getClass(), key), ".sonar"));

    // CHECK MODULES
    List<ProjectDefinition> modules = rootProject.getSubProjects();
    assertThat(modules).hasSize(2);

    // Module 1
    ProjectDefinition module1 = modules.get(0);
    assertThat(module1.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), key + "/module1"));
    assertThat(module1.getKey()).isEqualTo("com.foo.project:module1");
    assertThat(module1.getName()).isEqualTo("module1");
    assertThat(module1.getVersion()).isEqualTo("1.0-SNAPSHOT");
    // Description should not be inherited from parent if not set
    assertThat(module1.getDescription()).isNull();
    assertThat(module1.sources()).contains("sources");
    assertThat(module1.tests()).contains("tests");
    // and module properties must have been cleaned
    assertThat(module1.properties().get("module1.sonar.projectKey")).isNull();
    assertThat(module1.properties().get("module2.sonar.projectKey")).isNull();
    // Check baseDir and workDir
    assertThat(module1.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), key + "/module1"));
    assertThat(module1.getWorkDir().getCanonicalFile()).isEqualTo(new File(getResource(this.getClass(), key), ".sonar/com.foo.project_module1"));

    // Module 2
    ProjectDefinition module2 = modules.get(1);
    assertThat(module2.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), key + "/module2"));
    assertThat(module2.getKey()).isEqualTo("com.foo.project:com.foo.project.module2");
    assertThat(module2.getName()).isEqualTo("Foo Module 2");
    assertThat(module2.getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(module2.getDescription()).isEqualTo("Description of Module 2");
    assertThat(module2.sources()).contains("src");
    assertThat(module2.tests()).contains("tests");
    // and module properties must have been cleaned
    assertThat(module2.properties().get("module1.sonar.projectKey")).isNull();
    assertThat(module2.properties().get("module2.sonar.projectKey")).isNull();
    // Check baseDir and workDir
    assertThat(module2.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), key + "/module2"));
    assertThat(module2.getWorkDir().getCanonicalFile()).isEqualTo(
      new File(getResource(this.getClass(), key), ".sonar/com.foo.project_com.foo.project.module2"));

    return rootProject;
  }

  // SONAR-4876
  @Test
  public void shouldDefineMultiModuleProjectWithModuleKey() {
    ProjectDefinition rootProject = loadProjectDefinition("multi-module-definitions-moduleKey");

    // CHECK ROOT
    // module properties must have been cleaned
    assertThat(rootProject.properties().get("module1.sonar.moduleKey")).isNull();
    assertThat(rootProject.properties().get("module2.sonar.moduleKey")).isNull();

    // CHECK MODULES
    List<ProjectDefinition> modules = rootProject.getSubProjects();
    assertThat(modules).hasSize(2);

    // Module 2
    ProjectDefinition module2 = modules.get(1);
    assertThat(module2.getKey()).isEqualTo("com.foo.project.module2");
  }

  // SONARPLUGINS-2421
  @Test
  public void shouldDefineMultiLanguageProjectWithDefinitionsAllInRootProject() throws IOException {
    ProjectDefinition rootProject = loadProjectDefinition("multi-language-definitions-all-in-root");

    // CHECK ROOT
    assertThat(rootProject.getKey()).isEqualTo("example");
    assertThat(rootProject.getName()).isEqualTo("Example");
    assertThat(rootProject.getVersion()).isEqualTo("1.0");

    // CHECK MODULES
    List<ProjectDefinition> modules = rootProject.getSubProjects();
    assertThat(modules).hasSize(2);

    // Module 1
    ProjectDefinition module1 = modules.get(0);
    assertThat(module1.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), "multi-language-definitions-all-in-root"));
    assertThat(module1.sources()).contains("src/main/java");
    // and module properties must have been cleaned
    assertThat(module1.getWorkDir().getCanonicalFile())
      .isEqualTo(new File(getResource(this.getClass(), "multi-language-definitions-all-in-root"), ".sonar/example_java-module"));

    // Module 2
    ProjectDefinition module2 = modules.get(1);
    assertThat(module2.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), "multi-language-definitions-all-in-root"));
    assertThat(module2.sources()).contains("src/main/groovy");
    // and module properties must have been cleaned
    assertThat(module2.getWorkDir().getCanonicalFile())
      .isEqualTo(new File(getResource(this.getClass(), "multi-language-definitions-all-in-root"), ".sonar/example_groovy-module"));
  }

  @Test
  public void shouldDefineMultiModuleProjectWithBaseDir() {
    ProjectDefinition rootProject = loadProjectDefinition("multi-module-with-basedir");
    List<ProjectDefinition> modules = rootProject.getSubProjects();
    assertThat(modules.size()).isOne();
    assertThat(modules.get(0).getKey()).isEqualTo("com.foo.project:com.foo.project.module1");
  }

  @Test
  public void shouldFailIfUnexistingModuleBaseDir() {
    assertThatThrownBy(() -> loadProjectDefinition("multi-module-with-unexisting-basedir"))
      .isInstanceOf(MessageException.class)
      .hasMessage("The base directory of the module 'module1' does not exist: "
        + getResource(this.getClass(), "multi-module-with-unexisting-basedir").getAbsolutePath() + File.separator + "module1");
  }

  @Test
  public void shouldFailIfUnexistingSourceFolderInheritedInMultimodule() {
    assertThatThrownBy(() -> loadProjectDefinition("multi-module-with-unexisting-source-dir"))
      .isInstanceOf(MessageException.class)
      .hasMessage("The folder 'unexisting-source-dir' does not exist for 'com.foo.project:module1' (base directory = "
        + getResource(this.getClass(), "multi-module-with-unexisting-source-dir").getAbsolutePath() + File.separator + "module1)");
  }

  @Test
  public void shouldFailIfExplicitUnexistingTestFolder() {
    assertThatThrownBy(() -> loadProjectDefinition("simple-project-with-unexisting-test-dir"))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("The folder 'tests' does not exist for 'com.foo.project' (base directory = "
        + getResource(this.getClass(), "simple-project-with-unexisting-test-dir").getAbsolutePath());
  }

  @Test
  public void shouldFailIfExplicitUnexistingTestFolderOnModule() {
    assertThatThrownBy(() -> loadProjectDefinition("multi-module-with-explicit-unexisting-test-dir"))
      .isInstanceOf(MessageException.class)
      .hasMessage("The folder 'tests' does not exist for 'module1' (base directory = "
        + getResource(this.getClass(), "multi-module-with-explicit-unexisting-test-dir").getAbsolutePath() + File.separator + "module1)");
  }

  @Test
  public void should_fail_with_asterisks_in_sources() {
    assertThatThrownBy(() -> loadProjectDefinition("simple-project-with-asterisks-in-sources"))
      .isInstanceOf(MessageException.class)
      .hasMessage(ProjectReactorBuilder.WILDCARDS_NOT_SUPPORTED);
  }

  @Test
  public void should_fail_with_asterisks_in_tests() {
    assertThatThrownBy(() -> loadProjectDefinition("simple-project-with-asterisks-in-tests"))
      .isInstanceOf(MessageException.class)
      .hasMessage(ProjectReactorBuilder.WILDCARDS_NOT_SUPPORTED);
  }

  @Test
  public void multiModuleProperties() {
    ProjectDefinition projectDefinition = loadProjectDefinition("big-multi-module-definitions-all-in-root");

    assertThat(projectDefinition.properties().get("module11.property")).isNull();
    ProjectDefinition module1 = null;
    ProjectDefinition module2 = null;
    for (ProjectDefinition prj : projectDefinition.getSubProjects()) {
      if (prj.getKey().equals("com.foo.project:module1")) {
        module1 = prj;
      } else if (prj.getKey().equals("com.foo.project:module2")) {
        module2 = prj;
      }
    }
    assertThat(module1.properties().get("module11.property")).isNull();
    assertThat(module1.properties().get("property")).isNull();
    assertThat(module2.properties().get("module11.property")).isNull();
    assertThat(module2.properties().get("property")).isNull();

    ProjectDefinition module11 = null;
    ProjectDefinition module12 = null;
    for (ProjectDefinition prj : module1.getSubProjects()) {
      if (prj.getKey().equals("com.foo.project:module1:module11")) {
        module11 = prj;
      } else if (prj.getKey().equals("com.foo.project:module1:module12")) {
        module12 = prj;
      }
    }
    assertThat(module11.properties().get("module1.module11.property")).isNull();
    assertThat(module11.properties().get("module11.property")).isNull();
    assertThat(module11.properties()).containsEntry("property", "My module11 property");
    assertThat(module12.properties().get("module11.property")).isNull();
    assertThat(module12.properties().get("property")).isNull();
  }

  @Test
  public void shouldFailIfMandatoryPropertiesAreNotPresent() {
    Map<String, String> props = new HashMap<>();
    props.put("foo1", "bla");
    props.put("foo4", "bla");

    assertThatThrownBy(() -> ProjectReactorBuilder.checkMandatoryProperties(props, new String[] {"foo1", "foo2", "foo3"}))
      .isInstanceOf(MessageException.class)
      .hasMessage("You must define the following mandatory properties for 'Unknown': foo2, foo3");
  }

  @Test
  public void shouldFailIfMandatoryPropertiesAreNotPresentButWithProjectKey() {
    Map<String, String> props = new HashMap<>();
    props.put("foo1", "bla");
    props.put("sonar.projectKey", "my-project");

    assertThatThrownBy(() -> ProjectReactorBuilder.checkMandatoryProperties(props, new String[] {"foo1", "foo2", "foo3"}))
      .isInstanceOf(MessageException.class)
      .hasMessage("You must define the following mandatory properties for 'my-project': foo2, foo3");
  }

  @Test
  public void shouldNotFailIfMandatoryPropertiesArePresent() {
    Map<String, String> props = new HashMap<>();
    props.put("foo1", "bla");
    props.put("foo4", "bla");

    ProjectReactorBuilder.checkMandatoryProperties(props, new String[] {"foo1"});

    // No exception should be thrown
  }

  @Test
  public void shouldGetRelativeFile() {
    assertThat(ProjectReactorBuilder.resolvePath(getResource(this.getClass(), "/"), "shouldGetFile/foo.properties"))
      .isEqualTo(getResource(this.getClass(), "shouldGetFile/foo.properties"));
  }

  @Test
  public void shouldGetAbsoluteFile() {
    File file = getResource(this.getClass(), "shouldGetFile/foo.properties");

    assertThat(ProjectReactorBuilder.resolvePath(getResource(this.getClass(), "/"), file.getAbsolutePath()))
      .isEqualTo(file);
  }

  @Test
  public void shouldMergeParentProperties() {
    // Use a random value to avoid VM optimization that would create constant String and make s1 and s2 the same object
    int i = (int) Math.random() * 10;
    String s1 = "value" + i;
    String s2 = "value" + i;
    Map<String, String> parentProps = new HashMap<>();
    parentProps.put("toBeMergeProps", "fooParent");
    parentProps.put("existingChildProp", "barParent");
    parentProps.put("duplicatedProp", s1);
    parentProps.put("sonar.projectDescription", "Desc from Parent");

    Map<String, String> childProps = new HashMap<>();
    childProps.put("existingChildProp", "barChild");
    childProps.put("otherProp", "tutuChild");
    childProps.put("duplicatedProp", s2);

    ProjectReactorBuilder.mergeParentProperties(childProps, parentProps);

    assertThat(childProps)
      .hasSize(4)
      .containsEntry("toBeMergeProps", "fooParent")
      .containsEntry("existingChildProp", "barChild")
      .containsEntry("otherProp", "tutuChild");
    assertThat(childProps.get("sonar.projectDescription")).isNull();
    assertThat(childProps.get("duplicatedProp")).isSameAs(parentProps.get("duplicatedProp"));
  }

  @Test
  public void shouldInitRootWorkDir() {
    ProjectReactorBuilder builder = new ProjectReactorBuilder(new ScannerProperties(emptyMap()),
      mock(AnalysisWarnings.class));
    File baseDir = new File("target/tmp/baseDir");

    File workDir = builder.initRootProjectWorkDir(baseDir, emptyMap());

    assertThat(workDir).isEqualTo(new File(baseDir, ".sonar"));
  }

  @Test
  public void shouldInitWorkDirWithCustomRelativeFolder() {
    Map<String, String> props = singletonMap("sonar.working.directory", ".foo");
    ProjectReactorBuilder builder = new ProjectReactorBuilder(new ScannerProperties(props),
      mock(AnalysisWarnings.class));
    File baseDir = new File("target/tmp/baseDir");

    File workDir = builder.initRootProjectWorkDir(baseDir, props);

    assertThat(workDir).isEqualTo(new File(baseDir, ".foo"));
  }

  @Test
  public void shouldInitRootWorkDirWithCustomAbsoluteFolder() {
    Map<String, String> props = singletonMap("sonar.working.directory", new File("src").getAbsolutePath());
    ProjectReactorBuilder builder = new ProjectReactorBuilder(new ScannerProperties(props),
      mock(AnalysisWarnings.class));
    File baseDir = new File("target/tmp/baseDir");

    File workDir = builder.initRootProjectWorkDir(baseDir, props);

    assertThat(workDir).isEqualTo(new File("src").getAbsoluteFile());
  }

  @Test
  public void shouldFailIf2ModulesWithSameKey() {
    Map<String, String> props = singletonMap("sonar.projectKey", "root");
    ProjectDefinition root = ProjectDefinition.create().setProperties(props);

    Map<String, String> props1 = singletonMap("sonar.projectKey", "mod1");
    root.addSubProject(ProjectDefinition.create().setProperties(props1));

    // Check uniqueness of a new module: OK
    Map<String, String> props2 = singletonMap("sonar.projectKey", "mod2");
    ProjectDefinition mod2 = ProjectDefinition.create().setProperties(props2);
    ProjectReactorBuilder.checkUniquenessOfChildKey(mod2, root);

    // Now, add it and check again
    root.addSubProject(mod2);

    assertThatThrownBy(() -> ProjectReactorBuilder.checkUniquenessOfChildKey(mod2, root))
      .isInstanceOf(MessageException.class)
      .hasMessage("Project 'root' can't have 2 modules with the following key: mod2");
  }

  @Test
  public void shouldAcceptNoProjectName() {
    ProjectDefinition rootProject = loadProjectDefinition("simple-project-with-missing-project-name");
    assertThat(rootProject.getOriginalName()).isNull();
    assertThat(rootProject.getName()).isEqualTo("com.foo.project");
  }

  @Test
  public void shouldSetModuleKeyIfNotPresent() {
    Map<String, String> props = new HashMap<>();
    props.put("sonar.projectVersion", "1.0");

    // should be set
    ProjectReactorBuilder.setModuleKeyAndNameIfNotDefined(props, "foo", "parent");
    assertThat(props)
      .containsEntry("sonar.moduleKey", "parent:foo")
      .containsEntry("sonar.projectName", "foo");

    // but not this 2nd time
    ProjectReactorBuilder.setModuleKeyAndNameIfNotDefined(props, "bar", "parent");
    assertThat(props)
      .containsEntry("sonar.moduleKey", "parent:foo")
      .containsEntry("sonar.projectName", "foo");
  }

  private ProjectDefinition loadProjectDefinition(String projectFolder) {
    Map<String, String> props = loadProps(projectFolder);
    ScannerProperties bootstrapProps = new ScannerProperties(props);
    ProjectReactor projectReactor = new ProjectReactorBuilder(bootstrapProps, mock(AnalysisWarnings.class)).execute();
    return projectReactor.getRoot();
  }

  protected static Properties toProperties(File propertyFile) {
    Properties propsFromFile = new Properties();
    try (FileInputStream fileInputStream = new FileInputStream(propertyFile)) {
      propsFromFile.load(fileInputStream);
    } catch (IOException e) {
      throw new IllegalStateException("Impossible to read the property file: " + propertyFile.getAbsolutePath(), e);
    }
    // Trim properties
    for (String propKey : propsFromFile.stringPropertyNames()) {
      propsFromFile.setProperty(propKey, StringUtils.trim(propsFromFile.getProperty(propKey)));
    }
    return propsFromFile;
  }

  private Map<String, String> loadProps(String projectFolder) {
    Map<String, String> props = new HashMap<>();
    Properties runnerProps = toProperties(getResource(this.getClass(), projectFolder + "/sonar-project.properties"));
    for (final String name : runnerProps.stringPropertyNames()) {
      props.put(name, runnerProps.getProperty(name));
    }
    props.put("sonar.projectBaseDir", getResource(this.getClass(), projectFolder).getAbsolutePath());
    return props;
  }

  @Test
  public void shouldGetList() {
    Map<String, String> props = new HashMap<>();

    props.put("prop", "  foo  ,,  bar  , toto,tutu");
    assertThat(ProjectReactorBuilder.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

  @Test
  public void shouldGetListWithComma() {
    Map<String, String> props = new HashMap<>();

    props.put("prop", "\"foo,bar\",  toto,tutu");
    assertThat(ProjectReactorBuilder.getListFromProperty(props, "prop")).containsOnly("foo,bar", "toto", "tutu");
  }

  @Test
  public void shouldGetEmptyList() {
    Map<String, String> props = new HashMap<>();

    props.put("prop", "");
    assertThat(ProjectReactorBuilder.getListFromProperty(props, "prop")).isEmpty();
  }

  @Test
  public void shouldGetListFromFile() throws IOException {
    String filePath = "shouldGetList/foo.properties";
    Map<String, String> props = loadPropsFromFile(filePath);

    assertThat(ProjectReactorBuilder.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

  @Test
  public void doNotMixPropertiesWhenModuleKeyIsPrefixOfAnother() throws IOException {
    ProjectDefinition rootProject = loadProjectDefinition("multi-module-definitions-same-prefix");

    // CHECK ROOT
    assertThat(rootProject.getKey()).isEqualTo("com.foo.project");
    assertThat(rootProject.getName()).isEqualTo("Foo Project");
    assertThat(rootProject.getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(rootProject.getDescription()).isEqualTo("Description of Foo Project");
    assertThat(rootProject.sources()).contains("sources");
    assertThat(rootProject.tests()).contains("tests");
    // Module properties must have been cleaned
    assertThat(rootProject.properties().get("module1.sonar.projectKey")).isNull();
    assertThat(rootProject.properties().get("module2.sonar.projectKey")).isNull();
    // Check baseDir and workDir
    assertThat(rootProject.getBaseDir().getCanonicalFile())
      .isEqualTo(getResource(this.getClass(), "multi-module-definitions-same-prefix"));
    assertThat(rootProject.getWorkDir().getCanonicalFile())
      .isEqualTo(new File(getResource(this.getClass(), "multi-module-definitions-same-prefix"), ".sonar"));

    // CHECK MODULES
    List<ProjectDefinition> modules = rootProject.getSubProjects();
    assertThat(modules).hasSize(2);

    // Module 1
    ProjectDefinition module1 = modules.get(0);
    assertThat(module1.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), "multi-module-definitions-same-prefix/module1"));
    assertThat(module1.getKey()).isEqualTo("com.foo.project:module1");
    assertThat(module1.getName()).isEqualTo("module1");
    assertThat(module1.getVersion()).isEqualTo("1.0-SNAPSHOT");
    // Description should not be inherited from parent if not set
    assertThat(module1.getDescription()).isNull();
    assertThat(module1.sources()).contains("sources");
    assertThat(module1.tests()).contains("tests");
    // and module properties must have been cleaned
    assertThat(module1.properties().get("module1.sonar.projectKey")).isNull();
    assertThat(module1.properties().get("module2.sonar.projectKey")).isNull();
    // Check baseDir and workDir
    assertThat(module1.getBaseDir().getCanonicalFile())
      .isEqualTo(getResource(this.getClass(), "multi-module-definitions-same-prefix/module1"));
    assertThat(module1.getWorkDir().getCanonicalFile())
      .isEqualTo(new File(getResource(this.getClass(), "multi-module-definitions-same-prefix"), ".sonar/com.foo.project_module1"));

    // Module 1 Feature
    ProjectDefinition module1Feature = modules.get(1);
    assertThat(module1Feature.getBaseDir().getCanonicalFile()).isEqualTo(getResource(this.getClass(), "multi-module-definitions-same-prefix/module1.feature"));
    assertThat(module1Feature.getKey()).isEqualTo("com.foo.project:com.foo.project.module1.feature");
    assertThat(module1Feature.getName()).isEqualTo("Foo Module 1 Feature");
    assertThat(module1Feature.getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(module1Feature.getDescription()).isEqualTo("Description of Module 1 Feature");
    assertThat(module1Feature.sources()).contains("src");
    assertThat(module1Feature.tests()).contains("tests");
    // and module properties must have been cleaned
    assertThat(module1Feature.properties().get("module1.sonar.projectKey")).isNull();
    assertThat(module1Feature.properties().get("module2.sonar.projectKey")).isNull();
    // Check baseDir and workDir
    assertThat(module1Feature.getBaseDir().getCanonicalFile())
      .isEqualTo(getResource(this.getClass(), "multi-module-definitions-same-prefix/module1.feature"));
    assertThat(module1Feature.getWorkDir().getCanonicalFile())
      .isEqualTo(new File(getResource(this.getClass(), "multi-module-definitions-same-prefix"), ".sonar/com.foo.project_com.foo.project.module1.feature"));
  }

  private Map<String, String> loadPropsFromFile(String filePath) throws IOException {
    Properties props = new Properties();
    try (FileInputStream fileInputStream = new FileInputStream(getResource(this.getClass(), filePath))) {
      props.load(fileInputStream);
    }
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      result.put(entry.getKey().toString(), entry.getValue().toString());
    }
    return result;
  }

  /**
   * Search for a test resource in the classpath. For example getResource("org/sonar/MyClass/foo.txt");
   *
   * @param path the starting slash is optional
   * @return the resource. Null if resource not found
   */
  public static File getResource(String path) {
    String resourcePath = path;
    if (!resourcePath.startsWith("/")) {
      resourcePath = "/" + resourcePath;
    }
    URL url = ProjectReactorBuilderTest.class.getResource(resourcePath);
    if (url != null) {
      return FileUtils.toFile(url);
    }
    return null;
  }

  /**
   * Search for a resource in the classpath. For example calling the method getResource(getClass(), "myTestName/foo.txt") from
   * the class org.sonar.Foo loads the file $basedir/src/test/resources/org/sonar/Foo/myTestName/foo.txt
   *
   * @return the resource. Null if resource not found
   */
  public static File getResource(Class baseClass, String path) {
    String resourcePath = StringUtils.replaceChars(baseClass.getCanonicalName(), '.', '/');
    if (!path.startsWith("/")) {
      resourcePath += "/";
    }
    resourcePath += path;
    return getResource(resourcePath);
  }

}
