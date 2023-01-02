/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scm.svn;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnScmSupport {
  private SvnScmSupport() {
    // static only
  }

  static SVNClientManager newSvnClientManager(SvnConfiguration configuration) {
    ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
    final char[] passwordValue = getCharsOrNull(configuration.password());
    final char[] passPhraseValue = getCharsOrNull(configuration.passPhrase());
    ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(
      null,
      configuration.username(),
      passwordValue,
      configuration.privateKey(),
      passPhraseValue,
      false);
    return SVNClientManager.newInstance(options, authManager);
  }

  @CheckForNull
  private static char[] getCharsOrNull(@Nullable String s) {
    return s != null ? s.toCharArray() : null;
  }

  public static List<Object> getObjects() {
    return Arrays.asList(SvnScmProvider.class,
      SvnBlameCommand.class,
      SvnConfiguration.class
    );
  }
}
