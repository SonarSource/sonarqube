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
package org.sonar.server.computation.issue;

import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;
import org.sonar.server.util.cache.DiskCache;

import java.io.File;
import java.io.IOException;

/**
 * Cache of all the issues involved in the analysis. Their state is as it will be
 * persisted in database (after issue tracking, auto-assignment, ...)
 *
 */
public class FinalIssues extends DiskCache<DefaultIssue> {

  public FinalIssues(TempFolder tempFolder, System2 system2) throws IOException {
    super(tempFolder.newFile("issues", ".dat"), system2);
  }

  FinalIssues(File file, System2 system2) {
    super(file, system2);
  }
}
