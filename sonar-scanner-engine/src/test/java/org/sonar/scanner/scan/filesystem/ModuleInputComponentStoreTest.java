/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleInputComponentStoreTest {

  @TempDir
  private File projectBaseDir;

  @Mock
  BranchConfiguration branchConfiguration;

  @Mock
  SonarRuntime sonarRuntime;

  @Mock
  InputComponentStore mockedInputComponentStore;

  private InputComponentStore componentStore;
  private SensorContextTester sensorContextTester;

  private final String projectKey = "dummy key";

  @BeforeEach
  void setUp() {
    TestInputFileBuilder.newDefaultInputProject(projectKey, projectBaseDir);
    File moduleBaseDir = new File(projectBaseDir, "module");
    moduleBaseDir.mkdir();
    sensorContextTester = SensorContextTester.create(moduleBaseDir);
    componentStore = spy(new InputComponentStore(branchConfiguration, sonarRuntime));
  }

  @Test
  void should_cache_module_files_by_filename() {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    String filename = "some name";
    InputFile inputFile1 = new TestInputFileBuilder(projectKey, "module/some/path/" + filename).build();
    store.doAdd(inputFile1);

    InputFile inputFile2 = new TestInputFileBuilder(projectKey, "module/other/path/" + filename).build();
    store.doAdd(inputFile2);

    InputFile dummyInputFile = new TestInputFileBuilder(projectKey, "module/some/path/Dummy.java").build();
    store.doAdd(dummyInputFile);

    assertThat(store.getFilesByName(filename)).containsExactlyInAnyOrder(inputFile1, inputFile2);
  }

  @Test
  void should_cache_filtered_module_files_by_filename() {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    String filename = "some name";
    InputFile inputFile1 = new TestInputFileBuilder(projectKey, "some/path/" + filename).build();
    InputFile inputFile2 = new TestInputFileBuilder(projectKey, "module/other/path/" + filename).build();
    store.doAdd(inputFile2);

    when(componentStore.getFilesByName(filename)).thenReturn(List.of(inputFile1, inputFile2));

    assertThat(store.getFilesByName(filename)).containsOnly(inputFile2);
  }

  @Test
  void should_cache_module_files_by_filename_global_strategy() {
    ModuleInputComponentStore store = new ModuleInputComponentStore(sensorContextTester.module(), componentStore, new SensorStrategy());

    String filename = "some name";
    // None in the module
    InputFile inputFile1 = new TestInputFileBuilder(projectKey, "some/path/" + filename).build();
    InputFile inputFile2 = new TestInputFileBuilder(projectKey, "other/path/" + filename).build();

    when(componentStore.getFilesByName(filename)).thenReturn(List.of(inputFile1, inputFile2));

    assertThat(store.getFilesByName(filename)).containsExactlyInAnyOrder(inputFile1, inputFile2);
  }

  @Test
  void should_cache_module_files_by_extension() {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    InputFile inputFile1 = new TestInputFileBuilder(projectKey, "module/some/path/Program.java").build();
    store.doAdd(inputFile1);

    InputFile inputFile2 = new TestInputFileBuilder(projectKey, "module/other/path/Utils.java").build();
    store.doAdd(inputFile2);

    InputFile dummyInputFile = new TestInputFileBuilder(projectKey, "module/some/path/NotJava.cpp").build();
    store.doAdd(dummyInputFile);

    assertThat(store.getFilesByExtension("java")).containsExactlyInAnyOrder(inputFile1, inputFile2);
  }

  @Test
  void should_cache_filtered_module_files_by_extension() {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    InputFile inputFile1 = new TestInputFileBuilder(projectKey, "some/path/NotInModule.java").build();
    InputFile inputFile2 = new TestInputFileBuilder(projectKey, "module/some/path/Other.java").build();
    store.doAdd(inputFile2);

    when(componentStore.getFilesByExtension("java")).thenReturn(List.of(inputFile1, inputFile2));

    assertThat(store.getFilesByExtension("java")).containsOnly(inputFile2);
  }

  @Test
  void should_cache_module_files_by_extension_global_strategy() {
    ModuleInputComponentStore store = new ModuleInputComponentStore(sensorContextTester.module(), componentStore, new SensorStrategy());

    // None in the module
    InputFile inputFile1 = new TestInputFileBuilder(projectKey, "some/path/NotInModule.java").build();
    InputFile inputFile2 = new TestInputFileBuilder(projectKey, "some/path/Other.java").build();

    when(componentStore.getFilesByExtension("java")).thenReturn(List.of(inputFile1, inputFile2));

    assertThat(store.getFilesByExtension("java")).containsExactlyInAnyOrder(inputFile1, inputFile2);
  }

  @Test
  void should_not_cache_duplicates() {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    String ext = "java";
    String filename = "Program." + ext;
    InputFile inputFile = new TestInputFileBuilder(projectKey, "module/some/path/" + filename).build();
    store.doAdd(inputFile);
    store.doAdd(inputFile);
    store.doAdd(inputFile);

    assertThat(store.getFilesByName(filename)).containsExactly(inputFile);
    assertThat(store.getFilesByExtension(ext)).containsExactly(inputFile);
  }

  @Test
  void should_get_empty_iterable_on_cache_miss() {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    String ext = "java";
    String filename = "Program." + ext;
    InputFile inputFile = new TestInputFileBuilder(projectKey, "module/some/path/" + filename).build();
    store.doAdd(inputFile);

    assertThat(store.getFilesByName("nonexistent")).isEmpty();
    assertThat(store.getFilesByExtension("nonexistent")).isEmpty();
  }

  private ModuleInputComponentStore newModuleInputComponentStore() {
    SensorStrategy strategy = new SensorStrategy();
    strategy.setGlobal(false);
    return new ModuleInputComponentStore(sensorContextTester.module(), componentStore, strategy);
  }

  @Test
  void should_find_module_components_with_non_global_strategy() {
    SensorStrategy strategy = new SensorStrategy();
    ModuleInputComponentStore store = new ModuleInputComponentStore(sensorContextTester.module(), mockedInputComponentStore, strategy);

    strategy.setGlobal(false);

    store.inputFiles();
    verify(mockedInputComponentStore).filesByModule(sensorContextTester.module().key());

    String relativePath = "somepath";
    store.inputFile(relativePath);
    verify(mockedInputComponentStore).getFile(any(String.class), eq(relativePath));

    store.languages();
    verify(mockedInputComponentStore).languages(any(String.class));
  }

  @Test
  void should_find_all_components_with_global_strategy() {
    SensorStrategy strategy = new SensorStrategy();
    ModuleInputComponentStore store = new ModuleInputComponentStore(sensorContextTester.module(), mockedInputComponentStore, strategy);

    store.inputFiles();
    verify(mockedInputComponentStore).inputFiles();

    String relativePath = "somepath";
    store.inputFile(relativePath);
    verify(mockedInputComponentStore).inputFile(relativePath);

    store.languages();
    verify(mockedInputComponentStore).languages();
  }
}
