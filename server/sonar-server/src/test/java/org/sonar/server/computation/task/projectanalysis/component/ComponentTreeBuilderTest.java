/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.SnapshotDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.Project;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.newBuilder;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.DIRECTORY;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.FILE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.MODULE;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.PROJECT;
import static org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType.UNRECOGNIZED;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class ComponentTreeBuilderTest {

  private static final ComponentKeyGenerator KEY_GENERATOR = (module, component) -> "generated_"
    + ComponentKeys.createEffectiveKey(module.getKey(), component != null ? component.getPath() : null);
  private static final ComponentKeyGenerator PUBLIC_KEY_GENERATOR = (module, component) -> "public_"
    + ComponentKeys.createEffectiveKey(module.getKey(), component != null ? component.getPath() : null);
  private static final Function<String, String> UUID_SUPPLIER = (componentKey) -> componentKey + "_uuid";
  private static final EnumSet<ScannerReport.Component.ComponentType> REPORT_TYPES = EnumSet.of(PROJECT, MODULE, DIRECTORY, FILE);
  private static final String NO_SCM_BASE_PATH = "";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public ScannerComponentProvider scannerComponentProvider = new ScannerComponentProvider();

  private Project projectInDb = new Project(UUID_SUPPLIER.apply("K1"), "K1", "theProjectName");

  @Test
  public void build_throws_IAE_for_all_types_but_PROJECT_MODULE_DIRECTORY_FILE() {
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
            assertThat(e).hasMessage("Unsupported component type '" + type + "'");
          }
        });
  }

  @Test
  public void by_default_project_fields_are_loaded_from_report() {
    String nameInReport = "the name";
    String descriptionInReport = "the desc";
    Component root = call(newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(42)
      .setName(nameInReport)
      .setDescription(descriptionInReport)
      .setVersion("6.5")
      .build());

    assertThat(root.getUuid()).isEqualTo("generated_K1_uuid");
    assertThat(root.getKey()).isEqualTo("generated_K1");
    assertThat(root.getPublicKey()).isEqualTo("public_K1");
    assertThat(root.getType()).isEqualTo(Component.Type.PROJECT);
    assertThat(root.getName()).isEqualTo(nameInReport);
    assertThat(root.getDescription()).isEqualTo(descriptionInReport);
    assertThat(root.getReportAttributes().getRef()).isEqualTo(42);
    assertThat(root.getReportAttributes().getPath()).isNull();
    assertThat(root.getReportAttributes().getVersion()).isEqualTo("6.5");
    assertThatFileAttributesAreNotSet(root);
  }

  @Test
  public void project_name_is_loaded_from_db_if_absent_from_report() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build());

    assertThat(root.getName()).isEqualTo(projectInDb.getName());
  }

  @Test
  public void project_version_is_loaded_from_db_if_absent_from_report() {
    SnapshotDto baseAnalysis = new SnapshotDto().setVersion("6.5");
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build(), baseAnalysis);

    assertThat(root.getReportAttributes().getVersion()).isEqualTo("6.5");
  }

  @Test
  public void project_version_is_loaded_from_db_if_empty_report() {
    SnapshotDto baseAnalysis = new SnapshotDto().setVersion("6.5");
    Component root = call(newBuilder()
      .setType(PROJECT)
      .setVersion("")
      .build(), baseAnalysis);

    assertThat(root.getReportAttributes().getVersion()).isEqualTo("6.5");
  }

  @Test
  public void project_version_is_hardcoded_if_absent_from_report_and_db() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build());

    assertThat(root.getReportAttributes().getVersion()).isEqualTo("not provided");
  }

  @Test
  public void project_description_is_null_if_absent_from_report() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build());

    assertThat(root.getDescription()).isNull();
  }

  @Test
  public void project_scmPath_is_empty_if_scmBasePath_is_empty() {
    Component root = call(newBuilder()
      .setType(PROJECT)
      .build(), NO_SCM_BASE_PATH);

    assertThat(root.getReportAttributes().getScmPath()).isEmpty();
  }

  @Test
  public void any_component_with_projectRelativePath_has_this_value_as_scmPath_if_scmBasePath_is_empty() {
    String[] projectRelativePaths = {
      randomAlphabetic(4),
      randomAlphabetic(5),
      randomAlphabetic(6),
      randomAlphabetic(7)
    };
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .setProjectRelativePath(projectRelativePaths[0])
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .setKey("M")
      .setProjectRelativePath(projectRelativePaths[1])
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .setProjectRelativePath(projectRelativePaths[2])
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setProjectRelativePath(projectRelativePaths[3])
      .setLines(1));

    Component root = call(project, NO_SCM_BASE_PATH);

    assertThat(root.getReportAttributes().getScmPath())
      .contains(projectRelativePaths[0]);
    Component module = root.getChildren().iterator().next();
    assertThat(module.getReportAttributes().getScmPath())
      .contains(projectRelativePaths[1]);
    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getReportAttributes().getScmPath())
      .contains(projectRelativePaths[2]);
    Component file = directory.getChildren().iterator().next();
    assertThat(file.getReportAttributes().getScmPath())
      .contains(projectRelativePaths[3]);
  }

  @Test
  public void any_component_with_projectRelativePath_has_this_value_appended_to_scmBasePath_and_a_slash_as_scmPath_if_scmBasePath_is_not_empty() {
    String[] projectRelativePaths = {
      randomAlphabetic(4),
      randomAlphabetic(5),
      randomAlphabetic(6),
      randomAlphabetic(7)
    };
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .setProjectRelativePath(projectRelativePaths[0])
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .setKey("M")
      .setProjectRelativePath(projectRelativePaths[1])
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .setProjectRelativePath(projectRelativePaths[2])
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setProjectRelativePath(projectRelativePaths[3])
      .setLines(1));
    String scmBasePath = randomAlphabetic(10);

    Component root = call(project, scmBasePath);

    assertThat(root.getReportAttributes().getScmPath())
      .contains(scmBasePath + "/" + projectRelativePaths[0]);
    Component module = root.getChildren().iterator().next();
    assertThat(module.getReportAttributes().getScmPath())
      .contains(scmBasePath + "/" + projectRelativePaths[1]);
    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getReportAttributes().getScmPath())
      .contains(scmBasePath + "/" + projectRelativePaths[2]);
    Component file = directory.getChildren().iterator().next();
    assertThat(file.getReportAttributes().getScmPath())
      .contains(scmBasePath + "/" + projectRelativePaths[3]);
  }

  @Test
  public void keys_of_module_directory_and_file_are_generated() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .setKey("M")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);
    assertThat(root.getKey()).isEqualTo("generated_" + projectInDb.getKey());
    assertThat(root.getPublicKey()).isEqualTo("public_" + projectInDb.getKey());
    assertThat(root.getChildren()).hasSize(1);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getKey()).isEqualTo("generated_M");
    assertThat(module.getPublicKey()).isEqualTo("public_M");
    assertThat(module.getChildren()).hasSize(1);

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getKey()).isEqualTo("generated_M:src/js");
    assertThat(directory.getPublicKey()).isEqualTo("public_M:src/js");
    assertThat(directory.getChildren()).hasSize(1);

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getKey()).isEqualTo("generated_M:src/js/Foo.js");
    assertThat(file.getPublicKey()).isEqualTo("public_M:src/js/Foo.js");
    assertThat(file.getChildren()).isEmpty();
  }

  @Test
  public void names_of_module_directory_and_file_are_public_keys_if_names_are_absent_from_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .setKey("M")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getName()).isEqualTo("public_M");

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getName()).isEqualTo("public_M:src/js");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getName()).isEqualTo("public_M:src/js/Foo.js");
  }

  @Test
  public void names_of_module_directory_and_file_are_public_keys_if_names_are_empty_in_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey(projectInDb.getKey())
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .setKey("M")
      .setName("")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .setName("")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setName("")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getName()).isEqualTo("public_M");

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getName()).isEqualTo("public_M:src/js");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getName()).isEqualTo("public_M:src/js/Foo.js");
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
  public void keys_of_module_directory_and_files_includes_name_of_closest_module() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setKey("project 1")
      .setRef(1)
      .addChildRef(11).addChildRef(21).addChildRef(31).build();
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

    Component root = call(project);
    Map<Integer, Component> componentsByRef = indexComponentByRef(root);
    assertThat(componentsByRef.get(11).getKey()).isEqualTo("generated_module 1");
    assertThat(componentsByRef.get(11).getPublicKey()).isEqualTo("public_module 1");
    assertThat(componentsByRef.get(12).getKey()).isEqualTo("generated_module 2");
    assertThat(componentsByRef.get(12).getPublicKey()).isEqualTo("public_module 2");
    assertThat(componentsByRef.get(13).getKey()).isEqualTo("generated_module 3");
    assertThat(componentsByRef.get(13).getPublicKey()).isEqualTo("public_module 3");
    assertThat(componentsByRef.get(21).getKey()).startsWith("generated_project 1:");
    assertThat(componentsByRef.get(21).getPublicKey()).startsWith("public_project 1:");
    assertThat(componentsByRef.get(22).getKey()).startsWith("generated_module 1:");
    assertThat(componentsByRef.get(22).getPublicKey()).startsWith("public_module 1:");
    assertThat(componentsByRef.get(23).getKey()).startsWith("generated_module 2:");
    assertThat(componentsByRef.get(23).getPublicKey()).startsWith("public_module 2:");
    assertThat(componentsByRef.get(24).getKey()).startsWith("generated_module 3:");
    assertThat(componentsByRef.get(24).getPublicKey()).startsWith("public_module 3:");
    assertThat(componentsByRef.get(31).getKey()).startsWith("generated_project 1:");
    assertThat(componentsByRef.get(31).getPublicKey()).startsWith("public_project 1:");
    assertThat(componentsByRef.get(32).getKey()).startsWith("generated_module 1:");
    assertThat(componentsByRef.get(32).getPublicKey()).startsWith("public_module 1:");
    assertThat(componentsByRef.get(33).getKey()).startsWith("generated_module 2:");
    assertThat(componentsByRef.get(33).getPublicKey()).startsWith("public_module 2:");
    assertThat(componentsByRef.get(34).getKey()).startsWith("generated_module 3:");
    assertThat(componentsByRef.get(34).getPublicKey()).startsWith("public_module 3:");
    assertThat(componentsByRef.get(35).getKey()).startsWith("generated_project 1:");
    assertThat(componentsByRef.get(35).getPublicKey()).startsWith("public_project 1:");
    assertThat(componentsByRef.get(36).getKey()).startsWith("generated_module 1:");
    assertThat(componentsByRef.get(36).getPublicKey()).startsWith("public_module 1:");
    assertThat(componentsByRef.get(37).getKey()).startsWith("generated_module 2:");
    assertThat(componentsByRef.get(37).getPublicKey()).startsWith("public_module 2:");
    assertThat(componentsByRef.get(38).getKey()).startsWith("generated_module 3:");
    assertThat(componentsByRef.get(38).getPublicKey()).startsWith("public_module 3:");
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
      .setType(MODULE)
      .setKey("c2")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);
    assertThat(root.getUuid()).isEqualTo("generated_c1_uuid");

    Component module = root.getChildren().iterator().next();
    assertThat(module.getUuid()).isEqualTo("generated_c2_uuid");

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getUuid()).isEqualTo("generated_c2:src/js_uuid");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getUuid()).isEqualTo("generated_c2:src/js/Foo.js_uuid");
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
      .setType(MODULE)
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getDescription()).isNull();

    Component directory = module.getChildren().iterator().next();
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
      .setType(MODULE)
      .setDescription("")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setDescription("")
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setDescription("")
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getDescription()).isNull();

    Component directory = module.getChildren().iterator().next();
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
      .setType(MODULE)
      .setDescription("b")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setDescription("c")
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setDescription("d")
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getDescription()).isEqualTo("b");

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getDescription()).isEqualTo("c");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getDescription()).isEqualTo("d");
  }

  @Test
  public void versions_of_module_directory_and_file_are_set_from_report_if_present() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .setVersion("v1")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setVersion("v2")
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setVersion("v3")
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getReportAttributes().getVersion()).isEqualTo("v1");

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getReportAttributes().getVersion()).isEqualTo("v2");

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getReportAttributes().getVersion()).isEqualTo("v3");
  }

  @Test
  public void versions_of_module_directory_and_file_are_null_if_absent_from_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getReportAttributes().getVersion()).isNull();

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getReportAttributes().getVersion()).isNull();

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getReportAttributes().getVersion()).isNull();
  }

  @Test
  public void versions_of_module_directory_and_file_are_null_if_empty_in_report() {
    ScannerReport.Component project = newBuilder()
      .setType(PROJECT)
      .setRef(1)
      .addChildRef(2)
      .build();
    scannerComponentProvider.add(newBuilder()
      .setRef(2)
      .setType(MODULE)
      .setVersion("")
      .addChildRef(3));
    scannerComponentProvider.add(newBuilder()
      .setRef(3)
      .setType(DIRECTORY)
      .setVersion("")
      .setPath("src/js")
      .addChildRef(4));
    scannerComponentProvider.add(newBuilder()
      .setRef(4)
      .setType(FILE)
      .setVersion("")
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);

    Component module = root.getChildren().iterator().next();
    assertThat(module.getReportAttributes().getVersion()).isNull();

    Component directory = module.getChildren().iterator().next();
    assertThat(directory.getReportAttributes().getVersion()).isNull();

    Component file = directory.getChildren().iterator().next();
    assertThat(file.getReportAttributes().getVersion()).isNull();
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
      .setPath("src/js/Foo.js")
      .setLines(1));

    Component root = call(project);
    Component file = root.getChildren().iterator().next();
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
      .setPath("src/js/Foo.js")
      .setLines(1)
      .setLanguage(""));

    Component root = call(project);
    Component file = root.getChildren().iterator().next();
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
      .setPath("src/js/Foo.js")
      .setLines(1)
      .setLanguage("js")
      .setIsTest(true));

    Component root = call(project);
    Component file = root.getChildren().iterator().next();
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
      .setPath("src/js/Foo.js"));

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
      .setPath("src/js/Foo.js")
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
      .setPath("src/js/Foo.js")
      .setLines(-10));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File 'src/js/Foo.js' has no line");

    call(project);
  }

  private static class ScannerComponentProvider extends ExternalResource implements Function<Integer, ScannerReport.Component> {
    private final Map<Integer, ScannerReport.Component> components = new HashMap<>();

    @Override
    protected void before() {
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
    return call(project, NO_SCM_BASE_PATH);
  }

  private Component call(ScannerReport.Component project, String scmBasePath) {
    return newUnderTest(null).buildProject(project, scmBasePath);
  }

  private Component call(ScannerReport.Component project, @Nullable SnapshotDto baseAnalysis) {
    return call(project, baseAnalysis, NO_SCM_BASE_PATH);
  }

  private Component call(ScannerReport.Component project, @Nullable SnapshotDto baseAnalysis, String scmBasePath) {
    return newUnderTest(baseAnalysis).buildProject(project, scmBasePath);
  }

  private ComponentTreeBuilder newUnderTest(@Nullable SnapshotDto baseAnalysis) {
    return new ComponentTreeBuilder(KEY_GENERATOR, PUBLIC_KEY_GENERATOR, UUID_SUPPLIER, scannerComponentProvider, projectInDb, baseAnalysis);
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
