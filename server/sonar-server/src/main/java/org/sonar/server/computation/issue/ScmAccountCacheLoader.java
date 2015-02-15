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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.util.cache.CacheLoader;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Loads the association between a SCM account and a SQ user
 */
public class ScmAccountCacheLoader implements CacheLoader<String, String> {

  private final Logger log;
  private final UserIndex index;

  public ScmAccountCacheLoader(UserIndex index) {
    this(index, Loggers.get(ScmAccountCacheLoader.class));
  }

  @VisibleForTesting
  ScmAccountCacheLoader(UserIndex index, Logger log) {
    this.log = log;
    this.index = index;
  }

  @Override
  public String load(String scmAccount) {
    List<UserDoc> users = index.getAtMostThreeActiveUsersForScmAccount(scmAccount);
    if (users.size() == 1) {
      return users.get(0).login();
    }
    if (!users.isEmpty()) {
      // multiple users are associated to the same SCM account, for example
      // the same email
      Collection<String> logins = Collections2.transform(users, new Function<UserDoc, String>() {
        @Override
        public String apply(UserDoc input) {
          return input.login();
        }
      });
      log.warn(String.format("Multiple users share the SCM account '%s': %s", scmAccount, Joiner.on(", ").join(logins)));
    }
    return null;
  }

  @Override
  public Map<String, String> loadAll(Collection<? extends String> scmAccounts) {
    throw new UnsupportedOperationException("Loading by multiple scm accounts is not supported yet");
  }
}
