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
package org.sonar.application.command;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.application.es.EsInstallation;
import org.sonar.process.ProcessId;
import org.sonar.process.System2;

import static java.util.Objects.requireNonNull;

public abstract class AbstractCommand<T extends AbstractCommand> {
  // unique key among the group of commands to launch
  private final ProcessId id;
  private final Map<String, String> envVariables;
  private final Set<String> suppressedEnvVariables = new HashSet<>();
  private final File workDir;
  private EsInstallation esInstallation;

  protected AbstractCommand(ProcessId id, File workDir, System2 system2) {
    this.id = requireNonNull(id, "ProcessId can't be null");
    this.workDir = requireNonNull(workDir, "workDir can't be null");
    this.envVariables = new HashMap<>(system2.getenv());
  }

  public ProcessId getProcessId() {
    return id;
  }

  public File getWorkDir() {
    return workDir;
  }

  @SuppressWarnings("unchecked")
  private T castThis() {
    return (T) this;
  }

  public Map<String, String> getEnvVariables() {
    return envVariables;
  }

  public Set<String> getSuppressedEnvVariables() {
    return suppressedEnvVariables;
  }

  public T suppressEnvVariable(String key) {
    requireNonNull(key, "key can't be null");
    suppressedEnvVariables.add(key);
    envVariables.remove(key);
    return castThis();
  }

  public T setEnvVariable(String key, String value) {
    envVariables.put(
      requireNonNull(key, "key can't be null"),
      requireNonNull(value, "value can't be null"));
    return castThis();
  }


  public T setEsInstallation(EsInstallation esInstallation) {
    this.esInstallation = esInstallation;
    return castThis();
  }

  @CheckForNull
  public EsInstallation getEsInstallation() {
    return esInstallation;
  }
}
