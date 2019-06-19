/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.impl.utils;

import org.apache.commons.lang.StringUtils;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.api.utils.TempFolder;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Implementation of {@link org.sonar.api.utils.TempFolder} to be used
 * only in JUnit tests. It wraps {@link org.junit.rules.TemporaryFolder}.
 * <br>
 * Example:
 * <pre>
 * public class MyTest {
 *   &#064;@org.junit.Rule
 *   public JUnitTempFolder temp = new JUnitTempFolder();
 *
 *   &#064;@org.junit.Test
 *   public void myTest() throws Exception {
 *     File dir = temp.newDir();
 *     // ...
 *   }
 * }
 * </pre>
 *
 * @since 5.1
 */
public class JUnitTempFolder extends ExternalResource implements TempFolder {

  private final TemporaryFolder junit = new TemporaryFolder();

  @Override
  public Statement apply(Statement base, Description description) {
    return junit.apply(base, description);
  }

  @Override
  protected void before() throws Throwable {
    junit.create();
  }

  @Override
  protected void after() {
    junit.delete();
  }

  @Override
  public File newDir() {
    try {
      return junit.newFolder();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp dir", e);
    }
  }

  @Override
  public File newDir(String name) {
    try {
      return junit.newFolder(name);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp dir", e);
    }
  }

  @Override
  public File newFile() {
    try {
      return junit.newFile();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file", e);
    }
  }

  @Override
  public File newFile(@Nullable String prefix, @Nullable String suffix) {
    try {
      return junit.newFile(StringUtils.defaultString(prefix) + "-" + StringUtils.defaultString(suffix));
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file", e);
    }
  }
}
