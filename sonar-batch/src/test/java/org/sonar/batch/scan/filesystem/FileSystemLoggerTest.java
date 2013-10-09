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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.sonar.api.config.Settings;

import java.io.File;

import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FileSystemLoggerTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void log() {
    DefaultModuleFileSystem fs = new DefaultModuleFileSystem("foo", mock(Settings.class), mock(InputFileCache.class), mock(FileIndexer.class));
    File src = temp.newFolder("src");
    File test = temp.newFolder("test");
    File base = temp.newFolder("base");
    fs.setBaseDir(base);
    fs.addSourceDir(src);
    fs.addTestDir(test);

    Logger slf4j = mock(Logger.class);
    new FileSystemLogger(fs).doLog(slf4j);

    verify(slf4j).info(and(contains("Base dir:"), contains(base.getAbsolutePath())));
    verify(slf4j).info(and(contains("Source dirs:"), contains(src.getAbsolutePath())));
    verify(slf4j).info(and(contains("Test dirs:"), contains(test.getAbsolutePath())));
  }
}
