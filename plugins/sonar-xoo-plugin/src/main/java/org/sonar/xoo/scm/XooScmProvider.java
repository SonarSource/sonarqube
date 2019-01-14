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

import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.IgnoreCommand;
import org.sonar.api.batch.scm.ScmProvider;

import java.io.File;

public class XooScmProvider extends ScmProvider {

  private final XooBlameCommand blame;
  private XooIgnoreCommand ignore;

  public XooScmProvider(XooBlameCommand blame, XooIgnoreCommand ignore) {
    this.blame = blame;
    this.ignore = ignore;
  }

  @Override
  public boolean supports(File baseDir) {
    return new File(baseDir, ".xoo").exists();
  }

  @Override
  public String key() {
    return "xoo";
  }

  @Override
  public BlameCommand blameCommand() {
    return blame;
  }

  @Override
  public IgnoreCommand ignoreCommand() {
    return ignore;
  }
}
