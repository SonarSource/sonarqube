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
package org.sonar.ce.task.projectanalysis.issue;

import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;
import org.sonar.ce.task.projectanalysis.util.cache.ProtobufIssueDiskCache;

import javax.inject.Inject;
import java.io.File;

/**
 * Cache of all the issues involved in the analysis. Their state is as it will be
 * persisted in database (after issue tracking, auto-assignment, ...)
 */
public class ProtoIssueCache extends ProtobufIssueDiskCache {
  @Inject
  public ProtoIssueCache(TempFolder tempFolder, System2 system2) {
    super(tempFolder.newFile("issues", ".dat"), system2);
  }

  public ProtoIssueCache(File file, System2 system2) {
    super(file, system2);
  }
}
