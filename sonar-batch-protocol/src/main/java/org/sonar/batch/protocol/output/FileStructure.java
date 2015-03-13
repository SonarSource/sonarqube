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
package org.sonar.batch.protocol.output;

import java.io.File;

/**
 * Structure of files in the zipped report
 */
public class FileStructure {

  public static enum Domain {
    ISSUES("issues-"), ISSUES_ON_DELETED("issues-deleted-"), COMPONENT("component-"), MEASURES("measures-");

    private final String filePrefix;

    Domain(String filePrefix) {
      this.filePrefix = filePrefix;
    }
  }

  private final File dir;

  FileStructure(File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      throw new IllegalArgumentException("Directory of analysis report does not exist: " + dir);
    }
    this.dir = dir;
  }

  public File metadataFile() {
    return new File(dir, "metadata.pb");
  }

  public File fileFor(Domain domain, int componentRef) {
    return new File(dir, domain.filePrefix + componentRef + ".pb");
  }

}
