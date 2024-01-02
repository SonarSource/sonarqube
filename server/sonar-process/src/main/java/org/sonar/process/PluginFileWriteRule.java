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
package org.sonar.process;

import java.io.File;
import java.io.FilePermission;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PluginFileWriteRule implements PluginPolicyRule {
  private static final Set<String> BLOCKED_FILE_ACTIONS = new HashSet<>(Arrays.asList(
    "write",
    "delete",
    "execute"
  ));

  private final FilePermission blockedFilePermission;
  private final FilePermission tmpFilePermission;

  public PluginFileWriteRule(Path home, Path tmp) {
    blockedFilePermission = new FilePermission(home.toAbsolutePath().toString() + File.separatorChar + "-", String.join(",", BLOCKED_FILE_ACTIONS));
    tmpFilePermission = new FilePermission(tmp.toAbsolutePath().toString() + File.separatorChar + "-", String.join(",", BLOCKED_FILE_ACTIONS));
  }

  @Override
  public boolean implies(Permission permission) {
    if (permission instanceof FilePermission requestPermission) {
      return !blockedFilePermission.implies(requestPermission) || tmpFilePermission.implies(requestPermission);
    }
    return true;
  }
}
