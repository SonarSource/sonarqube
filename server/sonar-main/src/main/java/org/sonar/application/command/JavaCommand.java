/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.sonar.process.ProcessId;
import org.sonar.process.System2;

import static java.util.Objects.requireNonNull;

public class JavaCommand<T extends JvmOptions> extends AbstractCommand<JavaCommand<T>> {
  // program arguments
  private final Map<String, String> arguments = new LinkedHashMap<>();
  private final List<String> parameters = new LinkedList<>();
  // entry point
  private String className;
  private JvmOptions<T> jvmOptions;
  // relative path to JAR files
  private final List<String> classpath = new ArrayList<>();
  private boolean readsArgumentsFromFile;
  private Long gracefulStopTimeoutMs;

  public JavaCommand(ProcessId id, File workDir) {
    super(id, workDir, System2.INSTANCE);
  }

  public Map<String, String> getArguments() {
    return arguments;
  }

  public JavaCommand<T> setArgument(String key, @Nullable String value) {
    if (value == null) {
      arguments.remove(key);
    } else {
      arguments.put(key, value);
    }
    return this;
  }

  public List<String> getParameters() {
    return parameters;
  }

  public JavaCommand<T> addParameter(@Nullable String parameter) {
    if (parameter == null) {
      parameters.remove(parameter);
    } else {
      parameters.add(parameter);
    }
    return this;
  }

  public String getClassName() {
    return className;
  }

  public JavaCommand<T> setClassName(String className) {
    this.className = className;
    return this;
  }

  public JvmOptions<T> getJvmOptions() {
    return jvmOptions;
  }

  public JavaCommand<T> setJvmOptions(JvmOptions<T> jvmOptions) {
    this.jvmOptions = jvmOptions;

    return this;
  }

  public List<String> getClasspath() {
    return classpath;
  }

  public JavaCommand<T> addClasspath(String s) {
    classpath.add(s);
    return this;
  }

  public boolean getReadsArgumentsFromFile() {
    return readsArgumentsFromFile;
  }

  public JavaCommand<T> setReadsArgumentsFromFile(boolean readsArgumentsFromFile) {
    this.readsArgumentsFromFile = readsArgumentsFromFile;
    return this;
  }

  public JavaCommand<T> setArguments(Properties args) {
    for (Map.Entry<Object, Object> entry : args.entrySet()) {
      setArgument(entry.getKey().toString(), entry.getValue() != null ? entry.getValue().toString() : null);
    }
    return this;
  }

  public long getGracefulStopTimeoutMs() {
    requireNonNull(gracefulStopTimeoutMs, "gracefulStopTimeoutMs has not been set");
    return gracefulStopTimeoutMs;
  }

  public JavaCommand<T> setGracefulStopTimeoutMs(long gracefulStopTimeoutMs) {
    this.gracefulStopTimeoutMs = gracefulStopTimeoutMs;
    return this;
  }

  @Override
  public String toString() {
    return "JavaCommand{" + "workDir=" + getWorkDir() +
      ", arguments=" + arguments +
      ", className='" + className + '\'' +
      ", jvmOptions=" + jvmOptions +
      ", classpath=" + classpath +
      ", readsArgumentsFromFile=" + readsArgumentsFromFile +
      ", gracefulStopTimeoutMs=" + gracefulStopTimeoutMs +
      ", envVariables=" + getEnvVariables() +
      ", suppressedEnvVariables=" + getSuppressedEnvVariables() +
      '}';
  }
}
