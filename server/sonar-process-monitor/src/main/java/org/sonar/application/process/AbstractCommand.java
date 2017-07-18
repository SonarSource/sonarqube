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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.sonar.process.ProcessId;

public abstract class AbstractCommand<T extends AbstractCommand> {
  // unique key among the group of commands to launch
  private final ProcessId id;
  // program arguments
  private final Map<String, String> arguments = new LinkedHashMap<>();
  private final Map<String, String> envVariables = new HashMap<>(System.getenv());
  private File workDir;

  protected AbstractCommand(ProcessId id) {
    this.id = id;
  }

  public ProcessId getProcessId() {
    return id;
  }

  public File getWorkDir() {
    return workDir;
  }

  public T setWorkDir(File workDir) {
    this.workDir = workDir;
    return castThis();
  }

  @SuppressWarnings("unchecked")
  private T castThis() {
    return (T) this;
  }

  public Map<String, String> getArguments() {
    return arguments;
  }

  public T setArgument(String key, @Nullable String value) {
    if (value == null) {
      arguments.remove(key);
    } else {
      arguments.put(key, value);
    }
    return castThis();
  }

  public T setArguments(Properties args) {
    for (Map.Entry<Object, Object> entry : args.entrySet()) {
      setArgument(entry.getKey().toString(), entry.getValue() != null ? entry.getValue().toString() : null);
    }
    return castThis();
  }

  public Map<String, String> getEnvVariables() {
    return envVariables;
  }

  public T setEnvVariable(String key, @Nullable String value) {
    if (value == null) {
      envVariables.remove(key);
    } else {
      envVariables.put(key, value);
    }
    return castThis();
  }
}
