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

import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.util.cache.CacheLoader;

import java.util.Collection;
import java.util.Map;

public class RuleCacheLoader implements CacheLoader<RuleKey, RuleDto> {

  private final DbClient dbClient;

  public RuleCacheLoader(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public RuleDto load(RuleKey key) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.ruleDao().getNullableByKey(session, key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public Map<RuleKey, RuleDto> loadAll(Collection<? extends RuleKey> keys) {
    throw new UnsupportedOperationException("See RuleDao");
  }
}
