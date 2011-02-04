/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.api.resources;

import java.io.File;

/**
 * @since 2.6
 */
public interface InputFile {

  /**
   * The source base directory, different than the project basedir. For example in maven projects, the basedir of a source file stored in
   * src/main/java/org/foo/ is the directory src/main/java.
   */
  File getFileBaseDir();

  File getFile();

  /**
   * Path relative to basedir. Directory separator is slash '/', whatever the platform.
   * 
   * Example on windows: if file basedir is c:\project\src\ and file is c:\project\src\org\foo\Bar.java, then relative path
   * is org/foo/Bar.java
   *
   * Example on unix: if file basedir is /project/src and file is /project/src/org/foo/Bar.java, then relative path
   * is org/foo/Bar.java as well.
   */
  String getRelativePath();
}
