/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.issue;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.FluentIterable.from;

public class IssueDao implements Dao {

  private final MyBatis mybatis;

  public IssueDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void selectNonClosedIssuesByModule(long componentId, ResultHandler handler) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.select("org.sonar.db.issue.IssueMapper.selectNonClosedIssuesByModule", componentId, handler);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Optional<IssueDto> selectByKey(DbSession session, String key) {
    return Optional.fromNullable(mapper(session).selectByKey(key));
  }

  public IssueDto selectOrFailByKey(DbSession session, String key) {
    Optional<IssueDto> issue = selectByKey(session, key);
    if (!issue.isPresent()) {
      throw new RowNotFoundException(String.format("Issue with key '%s' does not exist", key));
    }
    return issue.get();
  }

  /**
   * Gets a list issues by their keys. The result does NOT contain {@code null} values for issues not found, so
   * the size of result may be less than the number of keys. A single issue is returned
   * if input keys contain multiple occurrences of a key.
   * <p>Results may be in a different order as input keys (see {@link #selectByOrderedKeys(DbSession, List)}).</p>
   */
  public List<IssueDto> selectByKeys(final DbSession session, List<String> keys) {
    return DatabaseUtils.executeLargeInputs(keys, new SelectByKeys(mapper(session)));
  }

  private static class SelectByKeys implements Function<List<String>, List<IssueDto>> {
    private final IssueMapper mapper;

    private SelectByKeys(IssueMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<IssueDto> apply(@Nonnull List<String> partitionOfKeys) {
      return mapper.selectByKeys(partitionOfKeys);
    }
  }

  /**
   * Gets a list issues by their keys. The result does NOT contain {@code null} values for issues not found, so
   * the size of result may be less than the number of keys. A single issue is returned
   * if input keys contain multiple occurrences of a key.
   * <p>Contrary to {@link #selectByKeys(DbSession, List)}, results are in the same order as input keys.</p>
   */
  public List<IssueDto> selectByOrderedKeys(DbSession session, List<String> keys) {
    List<IssueDto> unordered = selectByKeys(session, keys);
    return from(keys).transform(new KeyToIssue(unordered)).filter(Predicates.notNull()).toList();
  }

  private static class KeyToIssue implements Function<String, IssueDto> {
    private final Map<String, IssueDto> map = new HashMap<>();

    private KeyToIssue(Collection<IssueDto> unordered) {
      for (IssueDto dto : unordered) {
        map.put(dto.getKey(), dto);
      }
    }

    @Nullable
    @Override
    public IssueDto apply(@Nonnull String issueKey) {
      return map.get(issueKey);
    }
  }

  public Set<String> selectComponentUuidsOfOpenIssuesForProjectUuid(DbSession session, String projectUuid) {
    return mapper(session).selectComponentUuidsOfOpenIssuesForProjectUuid(projectUuid);
  }

  public void insert(DbSession session, IssueDto dto) {
    mapper(session).insert(dto);
  }

  public void insert(DbSession session, IssueDto dto, IssueDto... others) {
    IssueMapper mapper = mapper(session);
    mapper.insert(dto);
    for (IssueDto other : others) {
      mapper.insert(other);
    }
  }

  public void update(DbSession session, IssueDto dto) {
    mapper(session).update(dto);
  }

  private static IssueMapper mapper(DbSession session) {
    return session.getMapper(IssueMapper.class);
  }

}
