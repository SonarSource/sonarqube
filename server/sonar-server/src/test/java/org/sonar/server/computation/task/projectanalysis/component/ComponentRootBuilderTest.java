/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.newBuilder;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.DIRECTORY;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.FILE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.MODULE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.PROJECT;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.UNRECOGNIZED;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.UNSET;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentRootBuilder.createFileAttributes;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentRootBuilder.createOtherReportAttributes;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentRootBuilder.createProjectReportAttributes;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class ComponentRootBuilderTest {

  private static final Function<String, String> SIMPLE_UUID_GENERATOR = (componentKey) -> componentKey + "_uuid";
  private static final String NO_BRANCH = null;
  private static final String PROJECT_KEY = "this is the key";
  private static final String MODULE_KEY = "module key";
  private static final String DIRECTORY_PATH = "directory path";
  private static final String DIRECTORY_KEY = MODULE_KEY + ":" + DIRECTORY_PATH;
  private static final String FILE_PATH = "file path";
  private static final String FILE_KEY = MODULE_KEY + ":" + FILE_PATH;
  private static final ComponentDto PROJECT_DTO = new ComponentDto().setName("name in db");
  private static final Supplier<Optional<ComponentDto>> NO_COMPONENT_DTO_FOR_PROJECT = Optional::absent;
  private static final Function<String, Optional<SnapshotDto>> NO_BASEANALYSIS = (projectUuid) -> Optional.absent();
  private static final Supplier<Optional<ComponentDto>> COMPONENT_DTO_FOR_PROJECT = () -> Optional.of(PROJECT_DTO);
  private static final EnumSet<ScannerReport.Component.ComponentType> REPORT_TYPES = EnumSet.of(
    PROJECT, MODULE, DIRECTORY, FILE);
  private static final String PROJECT_UUID = "project uuid";
  private static final String DEFAULT_VERSION = "not provided";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public ScannerComponentProvider scannerComponentProvider = new ScannerComponentProvider();

  private ComponentRootBuilder underTest = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider, NO_COMPONENT_DTO_FOR_PROJECT, NO_BASEANALYSIS);

  @Test
  public void build_throws_IAE_for_all_types_but_PROJECT_MODULE_DIRECTORY_FILE() {
    Arrays.stream(ScannerReport.Component.ComponentType.values())
      .filter((type) -> type != UNRECOGNIZED)
      .filter((type) -> !REPORT_TYPES.contains(type))
      .forEach(
        (type) -> {
          ScannerReport.Component component = newBuilder().setType(type).build();
          try {
            underTest.build(component, "don't care");
            fail("Should have thrown a IllegalArgumentException");
          } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Unsupported component type '" + type + "'");
          }
        });
  }

  @Test
  public void name_of_project_is_name_in_Scanner_Component_when_set() {
    String expected = "the name";
    Component root = underTest.build(newBuilder().setType(PROJECT).setName(expected).build(), PROJECT_KEY);
    assertThat(root.getName()).isEqualTo(expected);
  }

  @Test
  public void name_of_project_is_name_in_Scanner_Component_when_set_even_if_there_is_a_ComponentDto() {
    String expected = "the name";
    Component root = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider, COMPONENT_DTO_FOR_PROJECT, NO_BASEANALYSIS)
      .build(newBuilder().setType(PROJECT).setName(expected).build(), PROJECT_KEY);
    assertThat(root.getName()).isEqualTo(expected);
  }

  @Test
  public void name_of_project_is_specified_key_when_name_is_unset_in_Scanner_Component_and_there_is_no_ComponentDto() {
    Component root = underTest.build(newBuilder().setType(PROJECT).build(), PROJECT_KEY);
    assertThat(root.getName()).isEqualTo(PROJECT_KEY);
  }

  @Test
  public void name_of_project_is_specified_key_when_name_is_empty_in_Scanner_Component_and_there_is_no_ComponentDto() {
    Component root = underTest.build(newBuilder().setType(PROJECT).setName("").build(), PROJECT_KEY);

    assertThat(root.getName()).isEqualTo(PROJECT_KEY);
  }

  @Test
  public void name_of_project_is_name_of_ComponentDto_when_name_is_unset_in_Scanner_Component_and_there_is_a_ComponentDto() {
    Component root = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider, COMPONENT_DTO_FOR_PROJECT, NO_BASEANALYSIS)
      .build(newBuilder().setType(PROJECT).build(), PROJECT_KEY);

    assertThat(root.getName()).isEqualTo(PROJECT_DTO.name());
  }

  @Test
  public void name_of_project_is_name_of_ComponentDto_when_name_is_empty_in_Scanner_Component_and_there_is_a_ComponentDto() {
    Component root = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider, COMPONENT_DTO_FOR_PROJECT, NO_BASEANALYSIS)
      .build(newBuilder().setType(PROJECT).setName("").build(), PROJECT_KEY);

    assertThat(root.getName()).isEqualTo(PROJECT_DTO.name());
  }

  @Test
  public void name_of_module_directory_and_file_contains_branch_when_non_empty() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).addChildRef(2).build();
    scannerComponentProvider.add(newBuilder().setRef(2).setType(MODULE).setKey(MODULE_KEY).addChildRef(3));
    scannerComponentProvider.add(newBuilder().setRef(3).setType(DIRECTORY).setPath(DIRECTORY_PATH).addChildRef(4));
    scannerComponentProvider.add(newBuilder().setRef(4).setType(FILE).setPath(FILE_PATH).setLines(1));

    String branch = "BRANCH";
    ComponentRootBuilder builder = new ComponentRootBuilder(branch, SIMPLE_UUID_GENERATOR, scannerComponentProvider, NO_COMPONENT_DTO_FOR_PROJECT, NO_BASEANALYSIS);

    Component root = builder.build(project, PROJECT_KEY);
    assertThat(root.getKey()).isEqualTo(PROJECT_KEY);
    assertThat(root.getChildren()).hasSize(1);
    Component module = root.getChildren().iterator().next();
    assertThat(module.getKey()).isEqualTo(MODULE_KEY + ":" + branch);
    assertThat(module.getChildren()).hasSize(1);
    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getKey()).isEqualTo(module.getKey() + ":" + DIRECTORY_PATH);
    assertThat(directory.getChildren()).hasSize(1);
    Component file = directory.getChildren().iterator().next();
    assertThat(file.getKey()).isEqualTo(module.getKey() + ":" + FILE_PATH);
    assertThat(file.getChildren()).isEmpty();
  }

  @Test
  public void name_of_module_directory_and_file_is_key_of_Scanner_Component_when_name_is_unset() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).addChildRef(2).build();
    scannerComponentProvider.add(newBuilder().setRef(2).setType(MODULE).setKey(MODULE_KEY).addChildRef(3));
    scannerComponentProvider.add(newBuilder().setRef(3).setType(DIRECTORY).setPath(DIRECTORY_PATH).addChildRef(4));
    scannerComponentProvider.add(newBuilder().setRef(4).setType(FILE).setPath(FILE_PATH).setLines(1));

    Component root = underTest.build(project, PROJECT_KEY);
    assertThat(root.getKey()).isEqualTo(PROJECT_KEY);
    Component module = root.getChildren().iterator().next();
    assertThat(module.getName()).isEqualTo(MODULE_KEY);
    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getName()).isEqualTo(module.getKey() + ":" + DIRECTORY_PATH);
    Component file = directory.getChildren().iterator().next();
    assertThat(file.getName()).isEqualTo(module.getKey() + ":" + FILE_PATH);
  }

  @Test
  public void name_of_module_directory_and_file_is_key_of_Scanner_Component_when_name_is_empty() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).setName("").addChildRef(2).build();
    scannerComponentProvider.add(newBuilder().setRef(2).setType(MODULE).setKey(MODULE_KEY).setName("").addChildRef(3));
    scannerComponentProvider.add(newBuilder().setRef(3).setType(DIRECTORY).setPath(DIRECTORY_PATH).setName("").addChildRef(4));
    scannerComponentProvider.add(newBuilder().setRef(4).setType(FILE).setPath(FILE_PATH).setName("").setLines(1));

    Component root = underTest.build(project, PROJECT_KEY);
    assertThat(root.getKey()).isEqualTo(PROJECT_KEY);
    Component module = root.getChildren().iterator().next();
    assertThat(module.getName()).isEqualTo(MODULE_KEY);
    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getName()).isEqualTo(module.getKey() + ":" + DIRECTORY_PATH);
    Component file = directory.getChildren().iterator().next();
    assertThat(file.getName()).isEqualTo(module.getKey() + ":" + FILE_PATH);
  }

  @Test
  public void name_of_module_directory_and_files_includes_name_of_closest_module() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).addChildRef(11).addChildRef(21).addChildRef(31).build();
    scannerComponentProvider.add(newBuilder().setRef(11).setType(MODULE).setKey("module 1").addChildRef(12).addChildRef(22).addChildRef(32));
    scannerComponentProvider.add(newBuilder().setRef(12).setType(MODULE).setKey("module 2").addChildRef(13).addChildRef(23).addChildRef(33));
    scannerComponentProvider.add(newBuilder().setRef(13).setType(MODULE).setKey("module 3").addChildRef(24).addChildRef(34));
    scannerComponentProvider.add(newBuilder().setRef(21).setType(DIRECTORY).setPath("directory in project").addChildRef(35));
    scannerComponentProvider.add(newBuilder().setRef(22).setType(DIRECTORY).setPath("directory in module 1").addChildRef(36));
    scannerComponentProvider.add(newBuilder().setRef(23).setType(DIRECTORY).setPath("directory in module 2").addChildRef(37));
    scannerComponentProvider.add(newBuilder().setRef(24).setType(DIRECTORY).setPath("directory in module 3").addChildRef(38));
    scannerComponentProvider.add(newBuilder().setRef(31).setType(FILE).setPath("file in project").setLines(1));
    scannerComponentProvider.add(newBuilder().setRef(32).setType(FILE).setPath("file in module 1").setLines(1));
    scannerComponentProvider.add(newBuilder().setRef(33).setType(FILE).setPath("file in module 2").setLines(1));
    scannerComponentProvider.add(newBuilder().setRef(34).setType(FILE).setPath("file in module 3").setLines(1));
    scannerComponentProvider.add(newBuilder().setRef(35).setType(FILE).setPath("file in directory in project").setLines(1));
    scannerComponentProvider.add(newBuilder().setRef(36).setType(FILE).setPath("file in directory in module 1").setLines(1));
    scannerComponentProvider.add(newBuilder().setRef(37).setType(FILE).setPath("file in directory in module 2").setLines(1));
    scannerComponentProvider.add(newBuilder().setRef(38).setType(FILE).setPath("file in directory in module 3").setLines(1));

    Component root = underTest.build(project, PROJECT_KEY);
    Map<Integer, Component> componentsByRef = indexComponentByRef(root);
    assertThat(componentsByRef.get(11).getKey()).isEqualTo("module 1");
    assertThat(componentsByRef.get(12).getKey()).isEqualTo("module 2");
    assertThat(componentsByRef.get(13).getKey()).isEqualTo("module 3");
    assertThat(componentsByRef.get(21).getKey()).startsWith(PROJECT_KEY + ":");
    assertThat(componentsByRef.get(22).getKey()).startsWith("module 1" + ":");
    assertThat(componentsByRef.get(23).getKey()).startsWith("module 2" + ":");
    assertThat(componentsByRef.get(24).getKey()).startsWith("module 3" + ":");
    assertThat(componentsByRef.get(31).getKey()).startsWith(PROJECT_KEY + ":");
    assertThat(componentsByRef.get(32).getKey()).startsWith("module 1" + ":");
    assertThat(componentsByRef.get(33).getKey()).startsWith("module 2" + ":");
    assertThat(componentsByRef.get(34).getKey()).startsWith("module 3" + ":");
    assertThat(componentsByRef.get(35).getKey()).startsWith(PROJECT_KEY + ":");
    assertThat(componentsByRef.get(36).getKey()).startsWith("module 1" + ":");
    assertThat(componentsByRef.get(37).getKey()).startsWith("module 2" + ":");
    assertThat(componentsByRef.get(38).getKey()).startsWith("module 3" + ":");
  }

  @Test
  public void version_of_project_is_set_to_default_value_when_unset_in_Scanner_Component_and_no_base_analysis() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).build();

    ComponentRootBuilder builder = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider,
      NO_COMPONENT_DTO_FOR_PROJECT, this::noBaseAnalysisButValidateProjectUuidArgument);

    Component root = builder.build(project, PROJECT_KEY);
    assertThat(root.getReportAttributes().getVersion()).isEqualTo(DEFAULT_VERSION);
  }

  @Test
  public void version_of_project_is_set_to_default_value_when_empty_in_Scanner_Component_and_no_base_analysis() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setVersion("").build();

    ComponentRootBuilder builder = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider,
      NO_COMPONENT_DTO_FOR_PROJECT, this::noBaseAnalysisButValidateProjectUuidArgument);

    Component root = builder.build(project, PROJECT_KEY);
    assertThat(root.getReportAttributes().getVersion()).isEqualTo(DEFAULT_VERSION);
  }

  private Optional<SnapshotDto> noBaseAnalysisButValidateProjectUuidArgument(String projectUuid) {
    assertThat(projectUuid).isEqualTo(SIMPLE_UUID_GENERATOR.apply(PROJECT_KEY));
    return Optional.absent();
  }

  @Test
  public void version_of_project_is_set_to_base_analysis_version_when_unset_in_Scanner_Component_and_base_analysis_has_a_version() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).build();

    String expected = "some version";
    ComponentRootBuilder builder = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider,
      NO_COMPONENT_DTO_FOR_PROJECT,
      (projectUuid) -> {
        assertThat(projectUuid).isEqualTo(SIMPLE_UUID_GENERATOR.apply(PROJECT_KEY));
        return Optional.of(new SnapshotDto().setVersion(expected));
      });

    Component root = builder.build(project, PROJECT_KEY);
    assertThat(root.getReportAttributes().getVersion()).isEqualTo(expected);
  }

  @Test
  public void version_of_project_is_set_to_base_analysis_version_when_empty_in_Scanner_Component_and_base_analysis_has_a_version() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setVersion("").build();

    String expected = "some version";
    ComponentRootBuilder builder = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider, NO_COMPONENT_DTO_FOR_PROJECT,
      (projectUuid) -> {
        assertThat(projectUuid).isEqualTo(SIMPLE_UUID_GENERATOR.apply(PROJECT_KEY));
        return Optional.of(new SnapshotDto().setVersion(expected));
      });

    Component root = builder.build(project, PROJECT_KEY);
    assertThat(root.getReportAttributes().getVersion()).isEqualTo(expected);
  }

  @Test
  public void version_of_project_is_set_to_default_value_when_unset_in_Scanner_Component_and_base_analysis_has_no_version() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).build();

    ComponentRootBuilder builder = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider,
      NO_COMPONENT_DTO_FOR_PROJECT,
      (projectUuid) -> {
        assertThat(projectUuid).isEqualTo(SIMPLE_UUID_GENERATOR.apply(PROJECT_KEY));
        return Optional.of(new SnapshotDto());
      });

    Component root = builder.build(project, PROJECT_KEY);
    assertThat(root.getReportAttributes().getVersion()).isEqualTo(DEFAULT_VERSION);
  }

  @Test
  public void version_of_project_is_set_to_default_value_when_empty_in_Scanner_Component_and_base_analysis_has_no_version() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setVersion("").build();

    ComponentRootBuilder builder = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider, NO_COMPONENT_DTO_FOR_PROJECT,
      (projectUuid) -> {
        assertThat(projectUuid).isEqualTo(SIMPLE_UUID_GENERATOR.apply(PROJECT_KEY));
        return Optional.of(new SnapshotDto());
      });

    Component root = builder.build(project, PROJECT_KEY);
    assertThat(root.getReportAttributes().getVersion()).isEqualTo(DEFAULT_VERSION);
  }

  @Test
  public void version_of_project_is_set_to_value_in_Scanner_Component_when_set() {
    String expected = "some version";
    ScannerReport.Component project = newBuilder().setType(PROJECT).setVersion(expected).build();
    ComponentRootBuilder builder = new ComponentRootBuilder(NO_BRANCH, SIMPLE_UUID_GENERATOR, scannerComponentProvider, NO_COMPONENT_DTO_FOR_PROJECT,
      this::noBaseAnalysisButEnsureIsNotCalled);

    assertThat(builder.build(project, PROJECT_KEY).getReportAttributes().getVersion()).isEqualTo(expected);
  }

  private Optional<SnapshotDto> noBaseAnalysisButEnsureIsNotCalled(String projectUuid) {
    fail("baseAnalysis provider should not have been called");
    return Optional.absent();
  }

  @Test
  public void uuid_is_value_from_uuid_supplier_for_project_module_directory_and_file() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).addChildRef(2).build();
    scannerComponentProvider.add(newBuilder().setRef(2).setType(MODULE).setKey(MODULE_KEY).addChildRef(3));
    scannerComponentProvider.add(newBuilder().setRef(3).setType(DIRECTORY).setPath(DIRECTORY_PATH).addChildRef(4));
    scannerComponentProvider.add(newBuilder().setRef(4).setType(FILE).setPath(FILE_PATH).setLines(1));

    Component root = underTest.build(project, PROJECT_KEY);
    Map<Integer, Component> componentByRef = indexComponentByRef(root);
    assertThat(componentByRef.get(1).getUuid()).isEqualTo(SIMPLE_UUID_GENERATOR.apply(PROJECT_KEY));
    assertThat(componentByRef.get(2).getUuid()).isEqualTo(SIMPLE_UUID_GENERATOR.apply(MODULE_KEY));
    assertThat(componentByRef.get(3).getUuid()).isEqualTo(SIMPLE_UUID_GENERATOR.apply(DIRECTORY_KEY));
    assertThat(componentByRef.get(4).getUuid()).isEqualTo(SIMPLE_UUID_GENERATOR.apply(FILE_KEY));

  }

  @Test
  public void description_of_project_module_directory_and_file_is_null_when_unset_in_Scanner_Component() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).addChildRef(2).build();
    scannerComponentProvider.add(newBuilder().setRef(2).setType(MODULE).addChildRef(3));
    scannerComponentProvider.add(newBuilder().setRef(3).setType(DIRECTORY).addChildRef(4));
    scannerComponentProvider.add(newBuilder().setRef(4).setType(FILE).setLines(1));

    Component root = underTest.build(project, PROJECT_KEY);
    Map<Integer, Component> componentByRef = indexComponentByRef(root);
    assertThat(componentByRef.get(1).getDescription()).isNull();
    assertThat(componentByRef.get(2).getDescription()).isNull();
    assertThat(componentByRef.get(3).getDescription()).isNull();
    assertThat(componentByRef.get(4).getDescription()).isNull();
  }

  @Test
  public void description_of_project_module_directory_and_file_is_null_when_empty_in_Scanner_Component() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).setDescription("").addChildRef(2).build();
    scannerComponentProvider.add(newBuilder().setRef(2).setType(MODULE).setDescription("").addChildRef(3));
    scannerComponentProvider.add(newBuilder().setRef(3).setType(DIRECTORY).setDescription("").addChildRef(4));
    scannerComponentProvider.add(newBuilder().setRef(4).setType(FILE).setLines(1).setDescription(""));

    Component root = underTest.build(project, PROJECT_KEY);
    Map<Integer, Component> componentByRef = indexComponentByRef(root);
    assertThat(componentByRef.get(1).getDescription()).isNull();
    assertThat(componentByRef.get(2).getDescription()).isNull();
    assertThat(componentByRef.get(3).getDescription()).isNull();
    assertThat(componentByRef.get(4).getDescription()).isNull();
  }

  @Test
  public void description_of_project_module_directory_and_file_is_description_of_Scanner_Component_when_set() {
    ScannerReport.Component project = newBuilder().setType(PROJECT).setRef(1).setDescription("desc of project").addChildRef(2).build();
    scannerComponentProvider.add(newBuilder().setRef(2).setType(MODULE).setDescription("desc of module").addChildRef(3));
    scannerComponentProvider.add(newBuilder().setRef(3).setType(DIRECTORY).setDescription("desc of directory").addChildRef(4));
    scannerComponentProvider.add(newBuilder().setRef(4).setType(FILE).setLines(1).setDescription("desc of file"));

    Component root = underTest.build(project, PROJECT_KEY);
    Map<Integer, Component> componentByRef = indexComponentByRef(root);
    assertThat(componentByRef.get(1).getDescription()).isEqualTo("desc of project");
    assertThat(componentByRef.get(2).getDescription()).isEqualTo("desc of module");
    assertThat(componentByRef.get(3).getDescription()).isEqualTo("desc of directory");
    assertThat(componentByRef.get(4).getDescription()).isEqualTo("desc of file");
  }

  @Test
  public void all_types_but_UNSET_and_UNRECOGNIZED_are_converted() {
    Arrays.stream(ScannerReport.Component.ComponentType.values())
      .filter((type) -> type != UNRECOGNIZED)
      .filter((type) -> type != UNSET)
      .forEach((type) -> assertThat(ComponentRootBuilder.convertType(type)).isEqualTo(Component.Type.valueOf(type.name())));
  }

  @Test
  public void createOtherReportAttributes_takes_ref_version_and_path_from_Scanner_Component() {
    int ref = 123;
    String version = "1.0";
    String path = "some path";

    ReportAttributes reportAttributes = createOtherReportAttributes(newBuilder()
      .setRef(ref)
      .setVersion(version)
      .setPath(path)
      .build());
    assertThat(reportAttributes.getRef()).isEqualTo(ref);
    assertThat(reportAttributes.getPath()).isEqualTo(path);
    assertThat(reportAttributes.getVersion()).isEqualTo(version);
  }

  @Test
  public void createOtherReportAttributes_sets_null_version_when_unset_in_Scanner_Component() {
    ReportAttributes reportAttributes = createOtherReportAttributes(newBuilder().build());
    assertThat(reportAttributes.getVersion()).isNull();
  }

  @Test
  public void createOtherReportAttributes_sets_null_version_when_empty_in_Scanner_Component() {
    ReportAttributes reportAttributes = createOtherReportAttributes(newBuilder().setVersion("").build());
    assertThat(reportAttributes.getVersion()).isNull();
  }

  @Test
  public void createOtherReportAttributes_sets_null_path_when_unset_in_Scanner_Component() {
    ReportAttributes reportAttributes = createOtherReportAttributes(newBuilder().build());
    assertThat(reportAttributes.getPath()).isNull();
  }

  @Test
  public void createOtherReportAttributes_sets_null_path_when_empty_in_Scanner_Component() {
    ReportAttributes reportAttributes = createOtherReportAttributes(newBuilder().setPath("").build());
    assertThat(reportAttributes.getPath()).isNull();
  }

  @Test
  public void createProjectReportAttributes_sets_null_path_when_unset_in_Scanner_Component() {
    ReportAttributes reportAttributes = createProjectReportAttributes(newBuilder().build(), PROJECT_UUID, NO_BASEANALYSIS);
    assertThat(reportAttributes.getPath()).isNull();
  }

  @Test
  public void createProjectReportAttributes_sets_null_path_when_empty_in_Scanner_Component() {
    ReportAttributes reportAttributes = createProjectReportAttributes(newBuilder().setPath("").build(), PROJECT_UUID, NO_BASEANALYSIS);
    assertThat(reportAttributes.getPath()).isNull();
  }

  @Test
  public void createFileAttributes_returns_null_when_type_is_not_FILE() {
    Arrays.stream(ScannerReport.Component.ComponentType.values())
      .filter((type) -> type != UNRECOGNIZED)
      .filter((type) -> type != FILE)
      .map(
        (type) -> newBuilder().setType(type).build())
      .forEach(
        (component) -> assertThat(createFileAttributes(component)).isNull());
  }

  @Test
  public void createFileAttributes_sets_language_to_null_when_unset_in_Scanner_Component() {
    assertThat(createFileAttributes(newBuilder().setType(FILE).setLines(1).build()).getLanguageKey()).isNull();
  }

  @Test
  public void createFileAttributes_sets_language_to_null_when_empty_in_Scanner_Component() {
    assertThat(createFileAttributes(newBuilder().setType(FILE).setLanguage("").setLines(1).build()).getLanguageKey()).isNull();
  }

  @Test
  public void createFileAttributes_sets_unitTest_from_Scanner_Component() {
    assertThat(createFileAttributes(newBuilder().setType(FILE).setLines(1).build()).isUnitTest()).isFalse();
    assertThat(createFileAttributes(newBuilder().setType(FILE).setIsTest(true).setLines(1).build()).isUnitTest()).isTrue();
  }

  @Test
  public void createFileAttributes_sets_lines_in_Scanner_Component() {
    assertThat(createFileAttributes(newBuilder().setType(FILE).setLines(10).build()).getLines()).isEqualTo(10);
  }

  @Test
  public void fail_with_IAE_when_createFileAttributes_lines_is_not_set() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File 'src/main/java/Main.java' has no line");
    createFileAttributes(newBuilder().setType(FILE).setPath("src/main/java/Main.java").build());
  }

  @Test
  public void fail_with_IAE_when_createFileAttributes_sets_lines_to_0() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File 'src/main/java/Main.java' has no line");
    createFileAttributes(newBuilder().setType(FILE).setPath("src/main/java/Main.java").setLines(0).build());
  }

  @Test
  public void fail_with_IAE_when_createFileAttributes_sets_lines_to_less_than_0() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File 'src/main/java/Main.java' has no line");
    createFileAttributes(newBuilder().setType(FILE).setPath("src/main/java/Main.java").setLines(-10).build());
  }

  private static class ScannerComponentProvider extends ExternalResource implements Function<Integer, ScannerReport.Component> {
    private final Map<Integer, ScannerReport.Component> components = new HashMap<>();

    @Override
    protected void before() throws Throwable {
      components.clear();
    }

    @Override
    public ScannerReport.Component apply(Integer componentRef) {
      return checkNotNull(components.get(componentRef), "No Component for componentRef %s", componentRef);
    }

    public ScannerReport.Component add(ScannerReport.Component.Builder builder) {
      ScannerReport.Component component = builder.build();
      ScannerReport.Component existing = components.put(component.getRef(), component);
      checkArgument(existing == null, "Component %s already set for ref %s", existing, component.getRef());
      return component;
    }
  }

  private static Map<Integer, Component> indexComponentByRef(Component root) {
    Map<Integer, Component> componentsByRef = new HashMap<>();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, PRE_ORDER) {
        @Override
        public void visitAny(Component any) {
          componentsByRef.put(any.getReportAttributes().getRef(), any);
        }
      }).visit(root);
    return componentsByRef;
  }
}
