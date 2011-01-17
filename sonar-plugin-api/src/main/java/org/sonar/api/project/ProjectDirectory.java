/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.project;

import java.io.File;
import java.util.List;

/**
 * Defines project directory in a form suitable for Sonar.
 * This is a part of bootstrap process, so we should take care about backward compatibility.
 * <p>
 * Couple of examples to show what this structure defines:
 * <ul>
 * <li>Typical Java project based on Ant might consist of two directories:
 * <ol>
 * <li>sources (location "src", output location "bin", includes "*.java")</li>
 * <li>resources (location "src", output location "bin", excludes "*.java")</li>
 * </ol>
 * </li>
 * <li>Typical Java project based on Maven might consist of four directories:
 * <ol>
 * <li>main sources (location "src/main/java", output location "target/classes")</li>
 * <li>main resources (location "src/main/resources", output location "target/classes")</li>
 * <li>test sources (location "src/test/java", output location "target/test-classes")</li>
 * <li>test resources (location "src/test/resources", output location "target/test-classes")</li>
 * </ol>
 * </li>
 * </ul>
 * </p>
 * 
 * @since 2.6
 */
public interface ProjectDirectory {

  /**
   * TODO We should find a more flexible way to specify kind.
   * Because actually this is just a logical division to be able to associate different quality profiles for different kinds of directories.
   * Imagine that we can have different rules for unit tests, integration tests, performance tests, UI tests and so on.
   * But seems that for now this enumeration would cover our needs.
   */
  public static enum Kind {
    SOURCES, RESOURCES, TESTS, TEST_RESOURCES
  }

  /**
   * @return kind of underlying files.
   */
  Kind getKind();

  /**
   * @return location of files for compilation.
   */
  File getLocation();

  /**
   * @return location of binary files after compilation.
   *         In case of Java this would be directory with Class files.
   */
  File getOutputLocation();

  /**
   * @return list of Ant-like inclusion patterns for files.
   */
  List<String> getInclusionPatterns();

  /**
   * @return list of Ant-like exclusion patterns for files.
   */
  List<String> getExclusionPatterns();

}
