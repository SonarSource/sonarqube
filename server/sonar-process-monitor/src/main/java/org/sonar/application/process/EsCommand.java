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
import java.util.ArrayList;
import java.util.List;
import org.sonar.process.ProcessId;

public class EsCommand extends AbstractCommand<EsCommand> {
  private File executable;
  private String url;
  private List<String> esOptions = new ArrayList<>();

  public EsCommand(ProcessId id) {
    super(id);
  }

  public File getExecutable() {
    return executable;
  }

  public EsCommand setExecutable(File executable) {
    this.executable = executable;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public EsCommand setUrl(String url) {
    this.url = url;
    return this;
  }

  public List<String> getEsOptions() {
    return esOptions;
  }

  public EsCommand addEsOption(String s) {
    if (!s.isEmpty()) {
      esOptions.add(s);
    }
    return this;
  }
}
