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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.RelativePathIndex;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.CachesTest;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class InputFileCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Caches caches;

  @Before
  public void start() throws Exception {
    caches = CachesTest.createCacheOnTemp(temp);
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_add_input_file() throws Exception {
    InputFileCache cache = new InputFileCache(caches);
    DefaultInputFile fooFile = new DefaultInputFile("src/main/java/Foo.java").setFile(temp.newFile("Foo.java"));
    cache.put("struts", fooFile);
    cache.put("struts-core", new DefaultInputFile("src/main/java/Bar.java").setFile(temp.newFile("Bar.java")));

    // index by relative path is automatically fed
    assertThat(cache.get("struts", RelativePathIndex.ID, "src/main/java/Foo.java").relativePath())
      .isEqualTo("src/main/java/Foo.java");

    assertThat(cache.byModule("struts")).hasSize(1);
    assertThat(cache.byModule("struts-core")).hasSize(1);
    assertThat(cache.all()).hasSize(2);
    for (InputFile inputFile : cache.all()) {
      assertThat(inputFile.relativePath()).startsWith("src/main/java/");
    }

    cache.remove("struts", fooFile);
    assertThat(cache.all()).hasSize(1);

    cache.removeModule("struts");
    assertThat(cache.byModule("struts")).hasSize(0);
    assertThat(cache.byModule("struts-core")).hasSize(1);
    assertThat(cache.all()).hasSize(1);
  }

  @Test
  public void only_relative_path_index_is_supported() throws Exception {
    InputFileCache cache = new InputFileCache(caches);
    DefaultInputFile input = new DefaultInputFile("src/main/java/Foo.java").setFile(temp.newFile("Foo.java"));

    try {
      cache.index("struts", "unsupported-index", "index-value", input);
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("Only relative path index is supported yet");
    }

    try {
      cache.get("struts", "unsupported-index", "index-value");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("Only relative path index is supported yet");
    }
  }
}
