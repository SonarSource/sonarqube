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
package org.sonar.application.process;

import java.util.ArrayList;
import java.util.List;
import org.sonar.process.ProcessId;

public class JavaCommand extends AbstractCommand<JavaCommand> {
  // entry point
  private String className;
  // for example -Xmx1G
  private final List<String> javaOptions = new ArrayList<>();
  // relative path to JAR files
  private final List<String> classpath = new ArrayList<>();

  public JavaCommand(ProcessId id) {
    super(id);
  }

  public List<String> getJavaOptions() {
    return javaOptions;
  }

  public JavaCommand addJavaOption(String s) {
    if (!s.isEmpty()) {
      javaOptions.add(s);
    }
    return this;
  }

  public JavaCommand addJavaOptions(String s) {
    for (String opt : s.split(" ")) {
      addJavaOption(opt);
    }
    return this;
  }

  public String getClassName() {
    return className;
  }

  public JavaCommand setClassName(String className) {
    this.className = className;
    return this;
  }

  public List<String> getClasspath() {
    return classpath;
  }

  public JavaCommand addClasspath(String s) {
    classpath.add(s);
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("JavaCommand{");
    sb.append("workDir=").append(getWorkDir());
    sb.append(", javaOptions=").append(javaOptions);
    sb.append(", className='").append(className).append('\'');
    sb.append(", classpath=").append(classpath);
    sb.append(", arguments=").append(getArguments());
    sb.append(", envVariables=").append(getEnvVariables());
    sb.append('}');
    return sb.toString();
  }
}
