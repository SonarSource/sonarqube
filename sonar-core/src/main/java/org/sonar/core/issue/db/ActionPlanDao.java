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

package org.sonar.core.issue.db;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
@BatchSide
@ServerSide
public class ActionPlanDao implements DaoComponent {

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

  public ActionPlanDto findByKey(String key) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActionPlanMapper.class).findByKey(key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActionPlanDto> findByKeys(Collection<String> keys) {
    if (keys.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession(false);
    try {
      List<ActionPlanDto> dtosList = newArrayList();
      List<List<String>> keysPartition = Lists.partition(newArrayList(keys), 1000);
      for (List<String> partition : keysPartition) {
        List<ActionPlanDto> dtos = session.getMapper(ActionPlanMapper.class).findByKeys(partition);
        dtosList.addAll(dtos);
      }
      return dtosList;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActionPlanDto> findOpenByProjectId(Long projectId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActionPlanMapper.class).findOpenByProjectId(projectId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActionPlanDto> findByNameAndProjectId(String name, Long projectId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActionPlanMapper.class).findByNameAndProjectId(name, projectId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
