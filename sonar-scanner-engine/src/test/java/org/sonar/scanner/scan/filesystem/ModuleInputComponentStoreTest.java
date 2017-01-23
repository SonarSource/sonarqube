/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ModuleInputComponentStoreTest {
  @Test
  public void should_cache_files_by_filename() throws IOException {
    ModuleInputComponentStore store = new ModuleInputComponentStore(mock(InputModule.class), new InputComponentStore());

    String filename = "some name";
    InputFile inputFile1 = new TestInputFileBuilder("dummy key", "some/path/" + filename).build();
    store.doAdd(inputFile1);

    InputFile inputFile2 = new TestInputFileBuilder("dummy key", "other/path/" + filename).build();
    store.doAdd(inputFile2);

    InputFile dummyInputFile = new TestInputFileBuilder("dummy key", "some/path/Dummy.java").build();
    store.doAdd(dummyInputFile);

    assertThat(store.getFilesByName(filename)).containsOnly(inputFile1, inputFile2);
  }
}
