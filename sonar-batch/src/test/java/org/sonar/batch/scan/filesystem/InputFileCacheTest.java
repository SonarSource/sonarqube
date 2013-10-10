/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.batch.index.Caches;

import static org.fest.assertions.Assertions.assertThat;

public class InputFileCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Caches caches = new Caches();

  @Before
  public void start() {
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_add_input_file() throws Exception {
    InputFileCache cache = new InputFileCache(caches);
    cache.put("struts", DefaultInputFile.create(temp.newFile(), "src/main/java/Foo.java", Maps.<String, String>newHashMap()));
    cache.put("struts-core", DefaultInputFile.create(temp.newFile(), "src/main/java/Foo.java", Maps.<String, String>newHashMap()));

    assertThat(cache.byModule("struts")).hasSize(1);
    assertThat(cache.byModule("struts-core")).hasSize(1);
    assertThat(cache.all()).hasSize(2);
    for (InputFile inputFile : cache.all()) {
      assertThat(inputFile.path()).startsWith("src/main/java");
    }
    assertThat(cache.fileRelativePaths("struts-core")).containsOnly("src/main/java/Foo.java");

    cache.removeModule("struts");
    assertThat(cache.byModule("struts")).hasSize(0);
    assertThat(cache.byModule("struts-core")).hasSize(1);
    assertThat(cache.all()).hasSize(1);
  }
}
