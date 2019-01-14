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
package org.sonar.xoo.scm;

import java.io.File;
import java.nio.file.Path;
import org.apache.commons.io.FilenameUtils;
import org.sonar.api.batch.scm.IgnoreCommand;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * To ignore a file simply add an empty file with the same name as the file to ignore with a .ignore suffix.
 * E.g. to ignore src/foo.xoo create the file src/foo.xoo.ignore
 */
public class XooIgnoreCommand implements IgnoreCommand {

  static final String IGNORE_FILE_EXTENSION = ".ignore";
  private static final Logger LOG = Loggers.get(XooIgnoreCommand.class);
  private boolean isInit;

  @Override
  public boolean isIgnored(Path path) {
    if (!isInit) {
      throw new IllegalStateException("Called init() first");
    }
    String fullPath = FilenameUtils.getFullPath(path.toAbsolutePath().toString());
    File scmIgnoreFile = new File(fullPath, path.getFileName() + IGNORE_FILE_EXTENSION);
    return scmIgnoreFile.exists();
  }

  @Override
  public void init(Path baseDir) {
    isInit = true;
    LOG.debug("Init IgnoreCommand on dir '{}'");
  }

  @Override
  public void clean() {
    LOG.debug("Clean IgnoreCommand");
  }
}
