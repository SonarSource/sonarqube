/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.process.monitor;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JavaCommand {

  // unique key among the group of commands to launch
  private final String key;

  private File workDir;

  // for example -Xmx1G
  private final List<String> javaOptions = new ArrayList<String>();

  // entry point
  private String className;

  // relative path to JAR files
  private final List<String> classpath = new ArrayList<String>();

  // program arguments (parameters of main(String[])
  private final Map<String, String> arguments = new LinkedHashMap<String, String>();

  private final Map<String, String> envVariables = new HashMap<String, String>(System.getenv());

  private File tempDir = null;

  private int processIndex = -1;

  public JavaCommand(String key) {
    this.key = key;
    processIndex = Monitor.getNextProcessId();
  }

  public String getKey() {
    return key;
  }

  public int getProcessIndex() {
    return processIndex;
  }

  public File getWorkDir() {
    return workDir;
  }

  public JavaCommand setWorkDir(File workDir) {
    this.workDir = workDir;
    return this;
  }

  public File getTempDir() {
    return tempDir;
  }

  public JavaCommand setTempDir(File tempDir) {
    this.tempDir = tempDir;
    return this;
  }

  public List<String> getJavaOptions() {
    return javaOptions;
  }

  public JavaCommand addJavaOption(String s) {
    if (StringUtils.isNotBlank(s)) {
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

  public Map<String, String> getArguments() {
    return arguments;
  }

  public JavaCommand setArgument(String key, @Nullable String value) {
    if (value == null) {
      arguments.remove(key);
    } else {
      arguments.put(key, value);
    }
    return this;
  }

  public JavaCommand setArguments(Properties args) {
    for (Map.Entry<Object, Object> entry : args.entrySet()) {
      setArgument(entry.getKey().toString(), entry.getValue() != null ? entry.getValue().toString() : null);
    }
    return this;
  }

  public Map<String, String> getEnvVariables() {
    return envVariables;
  }

  public JavaCommand setEnvVariable(String key, @Nullable String value) {
    if (value == null) {
      envVariables.remove(key);
    } else {
      envVariables.put(key, value);
    }
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("JavaCommand{");
    sb.append("workDir=").append(workDir);
    sb.append(", javaOptions=").append(javaOptions);
    sb.append(", className='").append(className).append('\'');
    sb.append(", classpath=").append(classpath);
    sb.append(", arguments=").append(arguments);
    sb.append(", envVariables=").append(envVariables);
    sb.append('}');
    return sb.toString();
  }
}
