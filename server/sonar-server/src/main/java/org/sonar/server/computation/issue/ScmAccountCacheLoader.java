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

import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.util.cache.CacheLoader;

import java.util.Collection;
import java.util.Map;

/**
 * Loads the association between a SCM account and a SQ user
 */
public class ScmAccountCacheLoader implements CacheLoader<String,String> {

  private final UserIndex index;

  public ScmAccountCacheLoader(UserIndex index) {
    this.index = index;
  }

  @Override
  public String load(String scmAccount) {
    UserDoc user = index.getNullableByScmAccount(scmAccount);
    return user != null ? user.login() : null;
  }

  @Override
  public Map<String, String> loadAll(Collection<? extends String> scmAccounts) {
    throw new UnsupportedOperationException("Loading by multiple scm accounts is not supported yet");
  }
}
