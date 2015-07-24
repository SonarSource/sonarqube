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
package org.sonar.db.rule;

import com.google.common.base.Function;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class RuleDao implements Dao {

  /**
   * Select rules by keys, whatever their status. Returns an empty list
   * if the list of {@code keys} is empty, without any db round trip.
   */
  public List<RuleDto> selectByKeys(final DbSession session, List<RuleKey> keys) {
    return executeLargeInputs(keys, new SelectByKeys(session.getMapper(RuleMapper.class)));
  }

  private static class SelectByKeys implements Function<List<RuleKey>, List<RuleDto>> {
    private final RuleMapper mapper;

    private SelectByKeys(RuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<RuleDto> apply(@Nonnull List<RuleKey> partitionOfKeys) {
      return mapper.selectByKeys(partitionOfKeys);
    }
  }

}
