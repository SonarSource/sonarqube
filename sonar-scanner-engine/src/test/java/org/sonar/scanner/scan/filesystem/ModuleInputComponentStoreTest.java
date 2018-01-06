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
package org.sonar.scanner.scan.filesystem;

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModuleInputComponentStoreTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private InputComponentStore componentStore;

  private final String moduleKey = "dummy key";

  @Before
  public void setUp() throws IOException {
    DefaultInputModule root = TestInputFileBuilder.newDefaultInputModule(moduleKey, temp.newFolder());
    componentStore = new InputComponentStore(root, mock(BranchConfiguration.class));
  }

  @Test
  public void should_cache_files_by_filename() throws IOException {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    String filename = "some name";
    InputFile inputFile1 = new TestInputFileBuilder(moduleKey, "some/path/" + filename).build();
    store.doAdd(inputFile1);

    InputFile inputFile2 = new TestInputFileBuilder(moduleKey, "other/path/" + filename).build();
    store.doAdd(inputFile2);

    InputFile dummyInputFile = new TestInputFileBuilder(moduleKey, "some/path/Dummy.java").build();
    store.doAdd(dummyInputFile);

    assertThat(store.getFilesByName(filename)).containsExactlyInAnyOrder(inputFile1, inputFile2);
  }

  @Test
  public void should_cache_files_by_extension() throws IOException {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    InputFile inputFile1 = new TestInputFileBuilder(moduleKey, "some/path/Program.java").build();
    store.doAdd(inputFile1);

    InputFile inputFile2 = new TestInputFileBuilder(moduleKey, "other/path/Utils.java").build();
    store.doAdd(inputFile2);

    InputFile dummyInputFile = new TestInputFileBuilder(moduleKey, "some/path/NotJava.cpp").build();
    store.doAdd(dummyInputFile);

    assertThat(store.getFilesByExtension("java")).containsExactlyInAnyOrder(inputFile1, inputFile2);
  }

  @Test
  public void should_not_cache_duplicates() throws IOException {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    String ext = "java";
    String filename = "Program." + ext;
    InputFile inputFile = new TestInputFileBuilder(moduleKey, "some/path/" + filename).build();
    store.doAdd(inputFile);
    store.doAdd(inputFile);
    store.doAdd(inputFile);

    assertThat(store.getFilesByName(filename)).containsExactly(inputFile);
    assertThat(store.getFilesByExtension(ext)).containsExactly(inputFile);
  }

  @Test
  public void should_get_empty_iterable_on_cache_miss() {
    ModuleInputComponentStore store = newModuleInputComponentStore();

    String ext = "java";
    String filename = "Program." + ext;
    InputFile inputFile = new TestInputFileBuilder(moduleKey, "some/path/" + filename).build();
    store.doAdd(inputFile);

    assertThat(store.getFilesByName("nonexistent")).isEmpty();
    assertThat(store.getFilesByExtension("nonexistent")).isEmpty();
  }

  private ModuleInputComponentStore newModuleInputComponentStore() {
    return new ModuleInputComponentStore(mock(InputModule.class), componentStore, mock(SensorStrategy.class));
  }

  @Test
  public void should_find_module_components_with_non_global_strategy() {
    InputComponentStore inputComponentStore = mock(InputComponentStore.class);
    SensorStrategy strategy = new SensorStrategy();
    InputModule module = mock(InputModule.class);
    when(module.key()).thenReturn("foo");
    ModuleInputComponentStore store = new ModuleInputComponentStore(module, inputComponentStore, strategy);

    store.inputFiles();
    verify(inputComponentStore).filesByModule("foo");

    String relativePath = "somepath";
    store.inputFile(relativePath);
    verify(inputComponentStore).getFile(any(String.class), eq(relativePath));

    store.inputDir(relativePath);
    verify(inputComponentStore).getDir(any(String.class), eq(relativePath));

    store.languages();
    verify(inputComponentStore).getLanguages(any(String.class));
  }

  @Test
  public void should_find_all_components_with_global_strategy() {
    InputComponentStore inputComponentStore = mock(InputComponentStore.class);
    SensorStrategy strategy = new SensorStrategy();
    ModuleInputComponentStore store = new ModuleInputComponentStore(mock(InputModule.class), inputComponentStore, strategy);

    strategy.setGlobal(true);

    store.inputFiles();
    verify(inputComponentStore).allFiles();

    String relativePath = "somepath";
    store.inputFile(relativePath);
    verify(inputComponentStore).getFile(relativePath);

    store.inputDir(relativePath);
    verify(inputComponentStore).getDir(relativePath);

    store.languages();
    verify(inputComponentStore).getLanguages();
  }
}
