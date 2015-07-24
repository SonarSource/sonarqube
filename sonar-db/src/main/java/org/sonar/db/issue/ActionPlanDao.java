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

package org.sonar.db.issue;

import com.google.common.base.Function;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class ActionPlanDao implements Dao {

  private final MyBatis mybatis;

  public ActionPlanDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void save(ActionPlanDto actionPlanDto) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.getMapper(ActionPlanMapper.class).insert(actionPlanDto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(ActionPlanDto actionPlanDto) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.getMapper(ActionPlanMapper.class).update(actionPlanDto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(String key) {
    SqlSession session = mybatis.openSession(false);
    try {
      session.getMapper(ActionPlanMapper.class).delete(key);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ActionPlanDto selectByKey(String key) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActionPlanMapper.class).findByKey(key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActionPlanDto> selectByKeys(Collection<String> keys) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByKeys(session, keys);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActionPlanDto> selectByKeys(DbSession dbSession, Collection<String> keys) {
    return DatabaseUtils.executeLargeInputs(keys, new SelectByKeys(dbSession.getMapper(ActionPlanMapper.class)));
  }

  private static class SelectByKeys implements Function<List<String>, List<ActionPlanDto>> {
    private final ActionPlanMapper mapper;

    private SelectByKeys(ActionPlanMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<ActionPlanDto> apply(@Nonnull List<String> partitionOfKeys) {
      return mapper.findByKeys(partitionOfKeys);
    }
  }

  public List<ActionPlanDto> selectOpenByProjectId(Long projectId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActionPlanMapper.class).findOpenByProjectId(projectId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActionPlanDto> selectByNameAndProjectId(String name, Long projectId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActionPlanMapper.class).findByNameAndProjectId(name, projectId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
