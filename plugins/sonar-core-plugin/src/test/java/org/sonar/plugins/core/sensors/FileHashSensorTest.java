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
package org.sonar.plugins.core.sensors;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.scan.filesystem.FileHashCache;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileHashSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private FileHashSensor sensor;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ModuleFileSystem fileSystem;

  private ComponentDataCache componentDataCache;

  private Project project;

  private FileHashCache fileHashCache;

  @Before
  public void prepare() {
    fileSystem = mock(ModuleFileSystem.class);
    componentDataCache = mock(ComponentDataCache.class);
    fileHashCache = mock(FileHashCache.class);
    sensor = new FileHashSensor(fileHashCache, fileSystem, new PathResolver(), componentDataCache);
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.language", "java");
    project = new Project("java_project").setConfiguration(conf).setLanguage(Java.INSTANCE);
  }

  @Test
  public void improve_code_coverage() throws Exception {
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    assertThat(sensor.toString()).isEqualTo("FileHashSensor");
  }

  @Test
  public void computeHashes() throws Exception {
    File baseDir = temp.newFolder();
    File file1 = new File(baseDir, "src/com/foo/Bar.java");
    FileUtils.write(file1, "Bar");
    when(fileHashCache.getCurrentHash(file1)).thenReturn("barhash");
    File file2 = new File(baseDir, "src/com/foo/Foo.java");
    FileUtils.write(file2, "Foo");
    when(fileHashCache.getCurrentHash(file2)).thenReturn("foohash");
    when(fileSystem.baseDir()).thenReturn(baseDir);
    when(fileSystem.files(any(FileQuery.class))).thenReturn(Arrays.asList(file1, file2)).thenReturn(Collections.<File> emptyList());
    sensor.analyse(project, mock(SensorContext.class));

    verify(componentDataCache).setStringData("java_project", "hash",
      "src/com/foo/Bar.java=barhash\n"
        + "src/com/foo/Foo.java=foohash\n");
  }
}
