/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.Project;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.DIRECTORY;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.FILE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.MODULE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.PROJECT;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.UNRECOGNIZED;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.newBuilder;

public class ComponentTreeBuilderTest {

  private static final ComponentKeyGenerator KEY_GENERATOR = (projectKey, path) -> "generated_"
    + ComponentKeys.createEffectiveKey(projectKey, path);
  private static final ComponentKeyGenerator PUBLIC_KEY_GENERATOR = (projectKey, path) -> "public_"
    + ComponentKeys.createEffectiveKey(projectKey, path);
  private static final Function<String, String> UUID_SUPPLIER = (componentKey) -> componentKey + "_uuid";
  private static final EnumSet<ScannerReport.Component.ComponentType> REPORT_TYPES = EnumSet.of(PROJECT, FILE);
  private static final String NO_SCM_BASE_PATH = "";
  // both no project as "" or null should be supported
  private static final ProjectAttributes SOME_PROJECT_ATTRIBUTES = new ProjectAttributes(
    randomAlphabetic(20), new Random().nextBoolean() ? null : randomAlphabetic(12));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public ScannerComponentProvider scannerComponentProvider = new ScannerComponentProvider();

  private Project projectInDb = Project.from(newPrivateProjectDto(newOrganizationDto(), UUID_SUPPLIER.apply("K1")).setDbKey("K1").setDescription(null));

  @Test
  public void build_throws_IAE_for_all_types_except_PROJECT_and_FILE() {
    Arrays.stream(ScannerReport.Component.ComponentType.values())
      .filter((type) -> type != UNRECOGNIZED)
      .filter((type) -> !REPORT_TYPES.contains(type))
      .forEach(
        (type) -> {
          scannerComponentProvider.clear();
          ScannerReport.Component project = newBuilder()
            .setType(PROJECT)
            .setKey(projectInDb.getKey())
            .setRef(1)
            .addChildRef(2)
            .setProjectRelativePath("root")
            .build();
          scannerComponentProvider.add(newBuilder()
            .setRef(2)
            .setType(type)
            .setProjectRelativePath("src")
            .setLines(1));
          try {
            call(project, NO_SCM_BASE_PATH, SOME_PROJECT_ATTRIBUTES);
            fail("Should have thrown a IllegalArgumentException");
          } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Unsupported component type '" + type + "'");
          }
        });
  }

  @Test
  public void build_throws_IAE_if_root_is_not_PROJECT() {
    Arrays.stream(ScannerReport.Component.ComponentType.values())
      .filter((type) -> type != UNRECOGNIZED)
      .filter((type) -> !REPORT_TYPES.contains(type))
      .forEach(
        (type) -> {
          ScannerReport.Component component = newBuilder().setType(type).build();
          try {
            call(component);
            fail("Should have thrown a IllegalArgumentException");
          } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Expected root component of type 'PROJECT'");
          }
        });
  }

  @Test
  public void by_default_project_fields_are_loaded_from_report() {
    String nameInReport = "the name";
    String descriptionInReport = "the desc";
    String buildString = randomAlphabetic(21);
    Component root = call(newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(42)
      .setName(nameInReport)
      .setDescription(descriptionInReport)
      .build(), NO_SCM_BASE_PATH, new ProjectAttributes("6.5", buildString));

    assertThat(root.getUuid()).isEqualTo("generated_K1_uuid");
    assertThat(root.getDbKey()).isEqualTo("generated_K1");
    assertThat(root.getKey()).isEqualTo("public_K1");
    assertThat(root.getType()).isEqualTo(Component.Type.PROJECT);
    assertThat(root.getName()).isEqualTo(nameInReport);
    assertThat(root.getShortName()).isEqualTo(nameInReport);
    assertThat(root.getDescription()).isEqualTo(descriptionInReport);
    assertThat(root.getReportAttributes().getRef()).isEqualTo(42);
    assertThat(root.getProjectAttributes().getProjectVersion()).contains("6.5");
    assertThat(root.getProjectAttributes().getBuildString()).isEqualTo(Optional.of(buildString));
    assertThatFileAttributesAreNotSet(root);
  }

  @Test
  public void project_name_is_loaded_from_db_if_absent_from_report() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build(), NO_SCM_BASE_PATH, SOME_PROJECT_ATTRIBUTES);

    assertThat(root.getName()).isEqualTo(projectInDb.getName());
  }

  @Test
  public void project_name_is_loaded_from_report_if_present_and_on_main_branch() {
    String reportName = randomAlphabetic(5);
    ScannerReport.Component reportProject = newBuilder()
      .setType(PROJECT)
      .setName(reportName)
      .build();

    Component root = newUnderTest(SOME_PROJECT_ATTRIBUTES, true).buildProject(reportProject, NO_SCM_BASE_PATH);

    assertThat(root.getName()).isEqualTo(reportName);
  }

  @Test
  public void project_name_is_loaded_from_db_if_not_on_main_branch() {
    String reportName = randomAlphabetic(5);
    ScannerReport.Component reportProject = newBuilder()
      .setType(PROJECT)
      .setName(reportName)
      .build();

    Component root = newUnderTest(SOME_PROJECT_ATTRIBUTES, false)
      .buildProject(reportProject, NO_SCM_BASE_PATH);

    assertThat(root.getName()).isEqualTo(projectInDb.getName());
  }

  @Test
  public void project_description_is_loaded_from_db_if_absent_from_report() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build(), NO_SCM_BASE_PATH, SOME_PROJECT_ATTRIBUTES);

    assertThat(root.getDescription()).isEqualTo(projectInDb.getDescription());
  }

  @Test
  public void project_description_is_loaded_from_report_if_present_and_on_main_branch() {
    String reportDescription = randomAlphabetic(5);
    ScannerReport.Component reportProject = newBuilder()
      .setType(PROJECT)
      .setDescription(reportDescription)
      .build();

    Component root = newUnderTest(SOME_PROJECT_ATTRIBUTES, true).buildProject(reportProject, NO_SCM_BASE_PATH);

    assertThat(root.getDescription()).isEqualTo(reportDescription);
  }

  @Test
  public void project_description_is_loaded_from_db_if_not_on_main_branch() {
    String reportDescription = randomAlphabetic(5);
    ScannerReport.Component reportProject = newBuilder()
      .setType(PROJECT)
      .setDescription(reportDescription)
      .build();

    Component root = newUnderTest(SOME_PROJECT_ATTRIBUTES, false).buildProject(reportProject, NO_SCM_BASE_PATH);

    assertThat(root.getDescription()).isEqualTo(projectInDb.getDescription());
  }

  @Test
  public void project_scmPath_is_empty_if_scmBasePath_is_empty() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build(), NO_SCM_BASE_PATH, SOME_PROJECT_ATTRIBUTES);

    assertThat(root.getReportAttributes().getScmPath()).isEmpty();
  }

  @Test
  public void projectAttributes_is_constructor_argument() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build(), NO_SCM_BASE_PATH, SOME_PROJECT_ATTRIBUTES);

    assertThat(root.getProjectAttributes()).isSameAs(SOME_PROJECT_ATTRIBUTES);
  }

  @Test
  public void any_component_with_projectRelativePath_has_this_value_as_scmPath_if_scmBasePath_is_empty() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .setProjectRelativePath("root")
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project, NO_SCM_BASE_PATH, SOME_PROJECT_ATTRIBUTES);

    assertThat(root.getReportAttributes().getScmPath())
      .contains("root");
    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getReportAttributes().getScmPath())
      .contains("src/js");
    Component file = directory.getChildren().iterator().next();
    assertThat(file.getReportAttributes().getScmPath())
      .contains("src/js/Foo.js");
  }

  @Test
  public void any_component_with_projectRelativePath_has_this_value_appended_to_scmBasePath_and_a_slash_as_scmPath_if_scmBasePath_is_not_empty() {
    ScannerReport.Component project = createProject();
    String scmBasePath = randomAlphabetic(10);

    Component root = call(project, scmBasePath, SOME_PROJECT_ATTRIBUTES);
    assertThat(root.getReportAttributes().getScmPath())
      .contains(scmBasePath);
    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getReportAttributes().getScmPath())
      .contains(scmBasePath + "/src/js");
    Component file = directory.getChildren().iterator().next();
    assertThat(file.getReportAttributes().getScmPath())
      .contains(scmBasePath + "/src/js/Foo.js");
  }

  private ScannerReport.Component createProject() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));
    return project;
  }

  @Test
  public void keys_of_directory_and_file_are_generated() {
    ScannerReport.Component project = createProject();

    Component root = call(project);
    assertThat(root.getDbKey()).isEqualTo("generated_" + projectInDb.getKey());
    assertThat(root.getKey()).isEqualTo("public_" + projectInDb.getKey());
    assertThat(root.getChildren()).hasSize(1);

    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getDbKey()).isEqualTo("generated_" + projectInDb.getKey() + ":src/js");
    assertThat(directory.getKey()).isEqualTo("public_" + projectInDb.getKey() + ":src/js");
    assertThat(directory.getChildren()).hasSize(1);

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getDbKey()).isEqualTo("generated_" + projectInDb.getKey() + ":src/js/Foo.js");
    assertThat(file.getKey()).isEqualTo("public_" + projectInDb.getKey() + ":src/js/Foo.js");
    assertThat(file.getChildren()).isEmpty();
  }

  @Test
  public void modules_are_not_created() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    List<Component> components = root.getChildren();
    assertThat(components).extracting("type").containsOnly(Component.Type.DIRECTORY);
  }

  @Test
  public void folder_hierarchy_is_created() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(4)
      .addChildRef(5)
      .addChildRef(6)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setProjectRelativePath("src/main/xoo/Foo1.js")
      .setLines(1));
    scannerComponentProvider.add(newBuilder()
      .setRef(5)
      .setType(FILE)
      .setProjectRelativePath("src/test/xoo/org/sonar/Foo2.js")
      .setLines(1));
    scannerComponentProvider.add(newBuilder()
      .setRef(6)
      .setType(FILE)
      .setProjectRelativePath("pom.xml")
      .setLines(1));

    Component root = call(project);
    assertThat(root.getChildren()).hasSize(2);

    Component pom = root.getChildren().get(1);
    assertThat(pom.getKey()).isEqualTo("public_K1:pom.xml");
    assertThat(pom.getName()).isEqualTo("pom.xml");

    Component directory = root.getChildren().get(0);
    assertThat(directory.getKey()).isEqualTo("public_K1:src");
    assertThat(directory.getName()).isEqualTo("src");

    // folders are collapsed and they only contain one directory
    Component d1 = directory.getChildren().get(0);
    assertThat(d1.getKey()).isEqualTo("public_K1:src/main/xoo");
    assertThat(d1.getName()).isEqualTo("src/main/xoo");
    assertThat(d1.getShortName()).isEqualTo("main/xoo");

    Component d2 = directory.getChildren().get(1);
    assertThat(d2.getKey()).isEqualTo("public_K1:src/test/xoo/org/sonar");
    assertThat(d2.getName()).isEqualTo("src/test/xoo/org/sonar");
    assertThat(d2.getShortName()).isEqualTo("test/xoo/org/sonar");
  }

  @Test
  public void collapse_directories_from_root() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/test/xoo/org/sonar/Foo2.js")
      .setLines(1));

    Component root = call(project);

    // folders are collapsed and they only contain one directory
    Component dir = root.getChildren().get(0);
    assertThat(dir.getKey()).isEqualTo("public_K1:src/test/xoo/org/sonar");
    assertThat(dir.getName()).isEqualTo("src/test/xoo/org/sonar");
    assertThat(dir.getShortName()).isEqualTo("src/test/xoo/org/sonar");

    Component file = dir.getChildren().get(0);
    assertThat(file.getKey()).isEqualTo("public_K1:src/test/xoo/org/sonar/Foo2.js");
    assertThat(file.getName()).isEqualTo("src/test/xoo/org/sonar/Foo2.js");
    assertThat(file.getShortName()).isEqualTo("Foo2.js");
  }

  @Test
  public void directories_are_collapsed() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getKey()).isEqualTo("public_K1:src/js");
    assertThat(directory.getName()).isEqualTo("src/js");
    assertThat(directory.getShortName()).isEqualTo("src/js");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getKey()).isEqualTo("public_K1:src/js/Foo.js");
    assertThat(file.getName()).isEqualTo("src/js/Foo.js");
    assertThat(file.getShortName()).isEqualTo("Foo.js");
  }

  @Test
  public void names_of_directory_and_file_are_based_on_the_path() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setName("")
      .setLines(1));

    Component root = call(project);

    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getName()).isEqualTo("src/js");
    assertThat(directory.getShortName()).isEqualTo("src/js");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getName()).isEqualTo("src/js/Foo.js");
    assertThat(file.getShortName()).isEqualTo("Foo.js");
  }

  @Test
  public void create_full_hierarchy_of_directories() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .addChildRef(3)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/java/Bar.java")
      .setName("")
      .setLines(2));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setName("")
      .setLines(1));

    Component root = call(project);

    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getKey()).isEqualTo("public_K1:src");
    assertThat(directory.getName()).isEqualTo("src");
    assertThat(directory.getShortName()).isEqualTo("src");

    Component directoryJava = directory.getChildren().get(0);
    assertThat(directoryJava.getKey()).isEqualTo("public_K1:src/java");
    assertThat(directoryJava.getName()).isEqualTo("src/java");
    assertThat(directoryJava.getShortName()).isEqualTo("java");

    Component directoryJs = directory.getChildren().get(1);
    assertThat(directoryJs.getKey()).isEqualTo("public_K1:src/js");
    assertThat(directoryJs.getName()).isEqualTo("src/js");
    assertThat(directoryJs.getShortName()).isEqualTo("js");

    Component file = directoryJs.getChildren().iterator().next();
    assertThat(file.getKey()).isEqualTo("public_K1:src/js/Foo.js");
    assertThat(file.getName()).isEqualTo("src/js/Foo.js");
    assertThat(file.getShortName()).isEqualTo("Foo.js");
  }

  private void assertThatFileAttributesAreNotSet(Component root) {
    try {
      root.getFileAttributes();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Only component of type FILE have a FileAttributes object");
    }
  }

  @Test
  public void keys_of_directory_and_files_includes_always_root_project() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey("project 1")
      .setRef(1)
      .addChildRef(31).build();
    scannerComponentProvider.add(newBuilder().setRef(31).setType(FILE).setProjectRelativePath("file in project").setLines(1));
    Component root = call(project);
    Map<String, Component> componentsByKey = indexComponentByKey(root);

    assertThat(componentsByKey.values()).extracting("key").startsWith("public_project 1");
    assertThat(componentsByKey.values()).extracting("dbKey").startsWith("generated_project 1");
  }

  @Test
  public void uuids_are_provided_by_supplier() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey("c1")
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);
    assertThat(root.getUuid()).isEqualTo("generated_c1_uuid");

    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getUuid()).isEqualTo("generated_c1:src/js_uuid");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getUuid()).isEqualTo("generated_c1:src/js/Foo.js_uuid");
  }

  @Test
  public void issues_are_relocated_from_directories_and_modules_to_root() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey("c1")
      .setRef(1)
      .addChildRef(2)
      .build();
    ScannerReport.Component.Builder file = newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1);
    scannerComponentProvider.add(file);

    call(project);
  }

  @Test
  public void descriptions_of_module_directory_and_file_are_null_if_absent_from_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getDescription()).isNull();

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getDescription()).isNull();
  }

  @Test
  public void descriptions_of_module_directory_and_file_are_null_if_empty_in_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .setDescription("")
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setDescription("")
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getDescription()).isNull();

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getDescription()).isNull();
  }

  @Test
  public void descriptions_of_module_directory_and_file_are_set_from_report_if_present() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setDescription("d")
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);
    Component directory = root.getChildren().iterator().next();
    assertThat(directory.getDescription()).isNull();

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getDescription()).isEqualTo("d");
  }

  @Test
  public void only_nb_of_lines_is_mandatory_on_file_attributes() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);
    Component dir = root.getChildren().iterator().next();
    Component file = dir.getChildren().iterator().next();
    assertThat(file.getFileAttributes().getLines()).isEqualTo(1);
    assertThat(file.getFileAttributes().getLanguageKey()).isNull();
    assertThat(file.getFileAttributes().isUnitTest()).isFalse();
  }

  @Test
  public void language_file_attributes_is_null_if_empty_in_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1)
      .setLanguage(""));

    Component root = call(project);
    Component dir2 = root.getChildren().iterator().next();

    Component file = dir2.getChildren().iterator().next();
    assertThat(file.getFileAttributes().getLanguageKey()).isNull();
  }

  @Test
  public void file_attributes_are_fully_loaded_from_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(1)
      .setLanguage("js")
      .setIsTest(true));

    Component root = call(project);
    Component dir = root.getChildren().iterator().next();
    Component file = dir.getChildren().iterator().next();
    assertThat(file.getFileAttributes().getLines()).isEqualTo(1);
    assertThat(file.getFileAttributes().getLanguageKey()).isEqualTo("js");
    assertThat(file.getFileAttributes().isUnitTest()).isTrue();
  }

  @Test
  public void throw_IAE_if_lines_is_absent_from_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File 'src/js/Foo.js' has no line");

    call(project);
  }

  @Test
  public void throw_IAE_if_lines_is_zero_in_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(0));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File 'src/js/Foo.js' has no line");

    call(project);
  }

  @Test
  public void throw_IAE_if_lines_is_negative_in_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(FILE)
      .setProjectRelativePath("src/js/Foo.js")
      .setLines(-10));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File 'src/js/Foo.js' has no line");

    call(project);
  }

  private static class ScannerComponentProvider extends ExternalResource implements Function<Integer, ScannerReport.Component> {
    private final Map<Integer, ScannerReport.Component> components = new HashMap<>();

    @Override
    protected void before() {
      clear();
    }

    public void clear() {
      components.clear();
    }

    @Override
    public ScannerReport.Component apply(Integer componentRef) {
      return Objects.requireNonNull(components.get(componentRef), "No Component for componentRef " + componentRef);
    }

    public ScannerReport.Component add(ScannerReport.Component.Builder builder) {
      ScannerReport.Component component = builder.build();
      ScannerReport.Component existing = components.put(component.getRef(), component);
      checkArgument(existing == null, "Component %s already set for ref %s", existing, component.getRef());
      return component;
    }
  }

  private Component call(ScannerReport.Component project) {
    return call(project, NO_SCM_BASE_PATH, SOME_PROJECT_ATTRIBUTES);
  }

  private Component call(ScannerReport.Component project, String scmBasePath, ProjectAttributes projectAttributes) {
    return newUnderTest(projectAttributes, true).buildProject(project, scmBasePath);
  }

  private ComponentTreeBuilder newUnderTest(ProjectAttributes projectAttributes, boolean mainBranch) {
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(mainBranch);
    return new ComponentTreeBuilder(KEY_GENERATOR, PUBLIC_KEY_GENERATOR, UUID_SUPPLIER, scannerComponentProvider,
      projectInDb, branch, projectAttributes);
  }

  private static Map<String, Component> indexComponentByKey(Component root) {
    Map<String, Component> componentsByKey = new HashMap<>();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, PRE_ORDER) {
        @Override
        public void visitAny(Component any) {
          componentsByKey.put(any.getDbKey(), any);
        }
      }).visit(root);
    return componentsByKey;
  }
}
