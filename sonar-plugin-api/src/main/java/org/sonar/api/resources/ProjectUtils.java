/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * @TODO Actually this class incorrectly named, because provides information not about project, but about Java project.
 *       And seems that only core plugins use this class.
 * 
 * @since 1.10
 */
public final class ProjectUtils {

  private ProjectUtils() {
    // utility class with only static methods
  }

  /**
   * @deprecated since 2.6 use JavaUtils.getTargetVersion() instead.
   */
  @Deprecated
  public static String getJavaVersion(Project project) {
    String version = project.getConfiguration() != null ? project.getConfiguration().getString("sonar.java.target") : null;
    return StringUtils.isNotBlank(version) ? version : "1.5";
  }

  /**
   * @deprecated since 2.6 use JavaUtils.getSourceVersion() instead.
   */
  @Deprecated
  public static String getJavaSourceVersion(Project project) {
    String version = project.getConfiguration() != null ? project.getConfiguration().getString("sonar.java.source") : null;
    return StringUtils.isNotBlank(version) ? version : "1.5";
  }

  /**
   * @since 2.7
   */
  public static List<java.io.File> toIoFiles(Collection<InputFile> inputFiles) {
    List<java.io.File> files = Lists.newArrayList();
    for (InputFile inputFile : inputFiles) {
      files.add(inputFile.getFile());
    }
    return files;
  }
}
