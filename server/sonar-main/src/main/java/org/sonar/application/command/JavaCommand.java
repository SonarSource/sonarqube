/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.application.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sonar.process.ProcessId;

public class JavaCommand<T extends JvmOptions> extends AbstractCommand<JavaCommand<T>> {
  // entry point
  private String className;
  private JvmOptions<T> jvmOptions;
  // relative path to JAR files
  private final List<String> classpath = new ArrayList<>();

  public JavaCommand(ProcessId id, File workDir) {
    super(id, workDir);
  }

  public JvmOptions<T> getJvmOptions() {
    return jvmOptions;
  }

  public JavaCommand<T> setJvmOptions(JvmOptions<T> jvmOptions) {
    this.jvmOptions = jvmOptions;

    return this;
  }

  public String getClassName() {
    return className;
  }

  public JavaCommand<T> setClassName(String className) {
    this.className = className;
    return this;
  }

  public List<String> getClasspath() {
    return classpath;
  }

  public JavaCommand<T> addClasspath(String s) {
    classpath.add(s);
    return this;
  }

  @Override
  public String toString() {
    return "JavaCommand{" + "workDir=" + getWorkDir() +
      ", jvmOptions=" + jvmOptions +
      ", className='" + className + '\'' +
      ", classpath=" + classpath +
      ", arguments=" + getArguments() +
      ", envVariables=" + getEnvVariables() +
      '}';
  }
}
