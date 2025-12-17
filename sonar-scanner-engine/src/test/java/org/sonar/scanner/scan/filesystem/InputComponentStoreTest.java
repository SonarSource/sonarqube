/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InputComponentStoreTest {

  @TempDir
  static File temp;

  @Test
  void should_add_input_file() {
    String rootModuleKey = "struts";
    String subModuleKey = "struts-core";

    File rootBaseDir = new File(temp, "rootBaseDir");
    assertThat(rootBaseDir.mkdir()).isTrue();

    ProjectDefinition moduleDef = ProjectDefinition.create()
      .setKey(subModuleKey).setBaseDir(rootBaseDir).setWorkDir(new File(temp, "moduleWorkDir"));
    ProjectDefinition rootDef = ProjectDefinition.create()
      .setKey(rootModuleKey).setBaseDir(rootBaseDir).setWorkDir(new File(temp, "rootWorkDir")).addSubProject(moduleDef);

    DefaultInputProject rootProject = TestInputFileBuilder.newDefaultInputProject(rootDef);
    DefaultInputModule subModule = TestInputFileBuilder.newDefaultInputModule(moduleDef);

    InputComponentStore store = new InputComponentStore(mock(BranchConfiguration.class));
    store.put(subModule);

    DefaultInputFile fooFile = new TestInputFileBuilder(rootModuleKey, "src/main/java/Foo.java")
      .setModuleBaseDir(rootBaseDir.toPath())
      .setPublish(true)
      .build();
    store.put(rootProject.key(), fooFile);
    File barModuleBaseDir = new File(temp, "barModuleBaseDir");
    assertThat(barModuleBaseDir.mkdir()).isTrue();
    store.put(subModuleKey, new TestInputFileBuilder(rootModuleKey, "src/main/java/Bar.java")
      .setLanguage("bla")
      .setPublish(false)
      .setType(Type.MAIN)
      .setStatus(Status.ADDED)
      .setLines(2)
      .setCharset(StandardCharsets.UTF_8)
      .setModuleBaseDir(barModuleBaseDir.toPath())
      .build());

    DefaultInputFile loadedFile = (DefaultInputFile) store.getFile(subModuleKey, "src/main/java/Bar.java");
    assertThat(loadedFile).isNotNull();
    assertThat(loadedFile.relativePath()).isEqualTo("src/main/java/Bar.java");
    assertThat(loadedFile.charset()).isEqualTo(StandardCharsets.UTF_8);

    assertThat(store.filesByModule(rootModuleKey)).hasSize(1);
    assertThat(store.filesByModule(subModuleKey)).hasSize(1);
    assertThat(store.inputFiles()).hasSize(2);
    for (InputPath inputPath : store.inputFiles()) {
      assertThat(inputPath.relativePath()).startsWith("src/main/java/");
    }

    List<InputFile> toPublish = new LinkedList<>();
    store.allFilesToPublish().forEach(toPublish::add);
    assertThat(toPublish).containsExactly(fooFile);
  }

  static class InputComponentStoreTester extends InputComponentStore {
    InputComponentStoreTester() {
      super(mock(BranchConfiguration.class));
    }

    InputFile addFile(String moduleKey, String relpath, @Nullable String language) {
      TestInputFileBuilder fileBuilder = new TestInputFileBuilder(moduleKey, relpath);
      ofNullable(language).ifPresent(fileBuilder::setLanguage);
      DefaultInputFile file = fileBuilder.build();
      put(moduleKey, file);
      return file;
    }

    InputFile addFile(String moduleKey, String relPath) {
      DefaultInputFile file = new TestInputFileBuilder(moduleKey, relPath)
        .build();
      put(moduleKey, file);
      return file;
    }

    InputFile addFile(String moduleKey, String relPath, @Nullable String language, boolean published) {
      TestInputFileBuilder fileBuilder = new TestInputFileBuilder(moduleKey, relPath);
      ofNullable(language).ifPresent(fileBuilder::setLanguage);
      fileBuilder.setPublish(published);
      DefaultInputFile file = fileBuilder.build();
      put(moduleKey, file);
      return file;
    }
  }

  @Test
  void should_add_languages_per_module_and_globally() {
    InputComponentStoreTester tester = new InputComponentStoreTester();

    String mod1Key = "mod1";
    tester.addFile(mod1Key, "src/main/java/Foo.java", "java");

    String mod2Key = "mod2";
    tester.addFile(mod2Key, "src/main/groovy/Foo.groovy", "groovy");

    assertThat(tester.languages(mod1Key)).containsExactly("java");
    assertThat(tester.languages(mod2Key)).containsExactly("groovy");
    assertThat(tester.languages()).containsExactlyInAnyOrder("java", "groovy");
  }

  @Test
  void should_find_files_per_module_and_globally() {
    InputComponentStoreTester tester = new InputComponentStoreTester();

    String mod1Key = "mod1";
    InputFile mod1File = tester.addFile(mod1Key, "src/main/java/Foo.java", "java");

    String mod2Key = "mod2";
    InputFile mod2File = tester.addFile(mod2Key, "src/main/groovy/Foo.groovy", "groovy");

    assertThat(tester.filesByModule(mod1Key)).containsExactly(mod1File);
    assertThat(tester.filesByModule(mod2Key)).containsExactly(mod2File);
    assertThat(tester.inputFiles()).containsExactlyInAnyOrder(mod1File, mod2File);
  }

  @Test
  void stores_analyzed_and_not_analyzed_indexed_file_count_per_extension() {
    InputComponentStoreTester underTest = new InputComponentStoreTester();
    String mod1Key = "mod1";

    underTest.addFile(mod1Key, "src/main/java/Foo.java", "java", true);
    underTest.addFile(mod1Key, "src/main/java/Bar.java", "java", true);
    underTest.addFile(mod1Key, "src/main/js/app.js", "js", true);
    underTest.addFile(mod1Key, "src/main/ts/module.ts", "ts", true);

    underTest.addFile(mod1Key, "src/main/js/util.js", null, false);
    String mod2Key = "mod2";
    underTest.addFile(mod2Key, "src/main/groovy/Foo.groovy", null, false);
    underTest.addFile(mod2Key, "src/test/groovy/FooTest.groovy", null, false);
    underTest.addFile(mod2Key, "src/main/py/script.py", null, false);

    assertThat(underTest.getAnalyzedIndexedFileCountPerExtension())
      .containsEntry("java", 2)
      .containsEntry("js", 1)
      .containsEntry("ts", 1)
      .hasSize(3);

    assertThat(underTest.getNotAnalyzedIndexedFileCountPerExtension())
      .containsEntry("js", 1)
      .containsEntry("groovy", 2)
      .containsEntry("py", 1)
      .hasSize(3);
  }
}
