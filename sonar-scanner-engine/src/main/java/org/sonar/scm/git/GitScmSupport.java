/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scm.git;

import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.util.FS;
import org.sonar.core.util.ProcessWrapperFactory;
import org.sonar.scm.git.strategy.DefaultBlameStrategy;

public final class GitScmSupport {
  private GitScmSupport() {
    // static only
  }

  public static List<Object> getObjects() {
    FS.FileStoreAttributes.setBackground(true);
    return Arrays.asList(
      JGitBlameCommand.class,
      CompositeBlameCommand.class,
      NativeGitBlameCommand.class,
      DefaultBlameStrategy.class,
      ProcessWrapperFactory.class,
      GitScmProvider.class,
      GitIgnoreCommand.class);
  }
}
