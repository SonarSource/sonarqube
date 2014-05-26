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

package org.sonar.wsclient.duplication.internal;

import org.sonar.wsclient.duplication.Duplication;
import org.sonar.wsclient.duplication.Duplications;
import org.sonar.wsclient.duplication.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultDuplications implements Duplications {

  private final List<Duplication> duplications = new ArrayList<Duplication>();
  private final Map<String, File> files = new HashMap<String, File>();

  @Override
  public List<Duplication> duplications() {
    return duplications;
  }

  @Override
  public List<File> files() {
    return new ArrayList<File>(files.values());
  }

  File fileByRef(String ref) {
    return files.get(ref);
  }

  DefaultDuplications addDuplication(Duplication duplication) {
    duplications.add(duplication);
    return this;
  }

  DefaultDuplications addFile(String ref, File file) {
    files.put(ref, file);
    return this;
  }

}
