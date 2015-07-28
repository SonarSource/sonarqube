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

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.util.cache.CacheLoader;

public class RuleCacheLoader implements CacheLoader<RuleKey, Rule> {

  private final DbClient dbClient;

  public RuleCacheLoader(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Rule load(RuleKey key) {
    DbSession session = dbClient.openSession(false);
    try {
      Optional<RuleDto> dto = dbClient.ruleDao().selectByKey(session, key);
      if (dto.isPresent()) {
        return new RuleImpl(dto.get());
      }
      return null;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public Map<RuleKey, Rule> loadAll(Collection<? extends RuleKey> keys) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
