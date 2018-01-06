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
package org.sonarqube.tests.plugins;

import com.google.common.base.Function;
import java.io.File;
import java.util.Collection;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import util.ItUtils;

import static com.google.common.collect.FluentIterable.from;

public class Project {

  public static File basedir() {
    return ItUtils.projectDir("plugins/project");
  }

  public static Iterable<String> allFilesInDir(final String dirPath) {
    Collection<File> files = FileUtils.listFiles(new File(basedir(), dirPath), null, true);
    return from(files).transform(new Function<File, String>() {
      @Nullable
      public String apply(File file) {
        // transforms /absolute/path/to/src/java/Foo.java to src/java/Foo.java
        String filePath = FilenameUtils.separatorsToUnix(file.getPath());
        return dirPath + StringUtils.substringAfterLast(filePath, dirPath);
      }
    });
  }
}
