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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.resources.*;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComponentIndexerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;
  private DefaultFileSystem fs;
  private SonarIndex sonarIndex;
  private AbstractLanguage cobolLanguage;
  private Project project;
  private ModuleFileSystemInitializer initializer;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    fs = new DefaultFileSystem(baseDir.toPath());
    sonarIndex = mock(SonarIndex.class);
    project = new Project("myProject");
    initializer = mock(ModuleFileSystemInitializer.class);
    when(initializer.baseDir()).thenReturn(baseDir);
    when(initializer.workingDir()).thenReturn(temp.newFolder());
    cobolLanguage = new AbstractLanguage("cobol") {
      @Override
      public String[] getFileSuffixes() {
        return new String[] {"cbl"};
      }
    };
  }

  @Test
  public void should_index_java_files() throws IOException {
    Languages languages = new Languages(Java.INSTANCE);
    ComponentIndexer indexer = createIndexer(languages);
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(project, null, mock(FileIndexer.class), initializer, indexer);
    fs.add(newInputFile("src/main/java/foo/bar/Foo.java", "", "foo/bar/Foo.java", "java", false));
    fs.add(newInputFile("src/main/java2/foo/bar/Foo.java", "", "foo/bar/Foo.java", "java", false));
    fs.add(newInputFile("src/test/java/foo/bar/FooTest.java", "", "foo/bar/FooTest.java", "java", true));

    fs.index();

    verify(sonarIndex).index(org.sonar.api.resources.File.create("src/main/java/foo/bar/Foo.java", Java.INSTANCE, false));
    verify(sonarIndex).index(org.sonar.api.resources.File.create("src/main/java2/foo/bar/Foo.java", Java.INSTANCE, false));
    verify(sonarIndex).index(argThat(new ArgumentMatcher<org.sonar.api.resources.File>() {
      @Override
      public boolean matches(Object arg0) {
        org.sonar.api.resources.File javaFile = (org.sonar.api.resources.File) arg0;
        return javaFile.getKey().equals("src/test/java/foo/bar/FooTest.java")
          && javaFile.getPath().equals("src/test/java/foo/bar/FooTest.java")
          && javaFile.getQualifier().equals(Qualifiers.UNIT_TEST_FILE);
      }
    }));
  }

  private ComponentIndexer createIndexer(Languages languages) {
    ResourceCache resourceCache = mock(ResourceCache.class);
    when(resourceCache.get(any(Resource.class))).thenReturn(new BatchResource(1, org.sonar.api.resources.File.create("foo.php"), null));
    return new ComponentIndexer(project, languages, sonarIndex, resourceCache);
  }

  @Test
  public void should_index_cobol_files() throws IOException {
    Languages languages = new Languages(cobolLanguage);
    ComponentIndexer indexer = createIndexer(languages);
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem(project, null, mock(FileIndexer.class), initializer, indexer);
    fs.add(newInputFile("src/foo/bar/Foo.cbl", "", "foo/bar/Foo.cbl", "cobol", false));
    fs.add(newInputFile("src2/foo/bar/Foo.cbl", "", "foo/bar/Foo.cbl", "cobol", false));
    fs.add(newInputFile("src/test/foo/bar/FooTest.cbl", "", "foo/bar/FooTest.cbl", "cobol", true));

    fs.index();

    verify(sonarIndex).index(org.sonar.api.resources.File.create("/src/foo/bar/Foo.cbl", cobolLanguage, false));
    verify(sonarIndex).index(org.sonar.api.resources.File.create("/src2/foo/bar/Foo.cbl", cobolLanguage, false));
    verify(sonarIndex).index(org.sonar.api.resources.File.create("/src/test/foo/bar/FooTest.cbl", cobolLanguage, true));
  }

  private DefaultInputFile newInputFile(String path, String content, String sourceRelativePath, String languageKey, boolean unitTest) throws IOException {
    File file = new File(baseDir, path);
    FileUtils.write(file, content);
    return new DeprecatedDefaultInputFile("foo", path)
      .setLanguage(languageKey)
      .setType(unitTest ? InputFile.Type.TEST : InputFile.Type.MAIN);
  }

}
