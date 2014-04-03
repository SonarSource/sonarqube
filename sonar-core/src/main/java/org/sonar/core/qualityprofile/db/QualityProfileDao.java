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

package org.sonar.core.qualityprofile.db;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.List;

public class QualityProfileDao implements ServerComponent {

  private final MyBatis mybatis;

  public QualityProfileDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void insert(QualityProfileDto dto, SqlSession session) {
    session.getMapper(QualityProfileMapper.class).insert(dto);
  }

  public void insert(QualityProfileDto dto) {
    SqlSession session = mybatis.openSession();
    try {
      insert(dto, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(QualityProfileDto dto, SqlSession session) {
    session.getMapper(QualityProfileMapper.class).update(dto);
  }

  public void update(QualityProfileDto dto) {
    SqlSession session = mybatis.openSession();
    try {
      update(dto, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(int id, SqlSession session) {
    session.getMapper(QualityProfileMapper.class).delete(id);
  }

  public void delete(int id) {
    SqlSession session = mybatis.openSession();
    try {
      delete(id, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectAll() {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectAll();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectDefaultProfile(String language, String key, SqlSession session) {
    return session.getMapper(QualityProfileMapper.class).selectDefaultProfile(language, key);
  }

  public QualityProfileDto selectDefaultProfile(String language, String key) {
    SqlSession session = mybatis.openSession();
    try {
      return selectDefaultProfile(language, key, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectByProjectAndLanguage(long projectId, String language, String key) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectByProjectAndLanguage(projectId, language, key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectByLanguage(String language) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectByLanguage(language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectById(int id, SqlSession session) {
    return session.getMapper(QualityProfileMapper.class).selectById(id);
  }

  @CheckForNull
  public QualityProfileDto selectById(int id) {
    SqlSession session = mybatis.openSession();
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectParent(int childId, SqlSession session) {
    return session.getMapper(QualityProfileMapper.class).selectParent(childId);
  }

  @CheckForNull
  public QualityProfileDto selectParent(int childId) {
    SqlSession session = mybatis.openSession();
    try {
      return selectParent(childId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectChildren(String name, String language, SqlSession session) {
    return session.getMapper(QualityProfileMapper.class).selectChildren(name, language);
  }

  public List<QualityProfileDto> selectChildren(String name, String language) {
    SqlSession session = mybatis.openSession();
    try {
      return selectChildren(name, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countChildren(String name, String language, SqlSession session) {
    return session.getMapper(QualityProfileMapper.class).countChildren(name, language);
  }

  public int countChildren(String name, String language) {
    SqlSession session = mybatis.openSession();
    try {
      return countChildren(name, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectByNameAndLanguage(String name, String language, SqlSession session) {
    return session.getMapper(QualityProfileMapper.class).selectByNameAndLanguage(name, language);
  }

  public QualityProfileDto selectByNameAndLanguage(String name, String language) {
    SqlSession session = mybatis.openSession();
    try {
      return selectByNameAndLanguage(name, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String propertyKey, String propertyValue) {
    SqlSession session = mybatis.openSession();
    try {
      return selectProjects(propertyKey, propertyValue, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String propertyKey, String propertyValue, SqlSession session) {
    return session.getMapper(QualityProfileMapper.class).selectProjects(propertyKey, propertyValue);
  }

  public int countProjects(String propertyKey, String propertyValue) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).countProjects(propertyKey, propertyValue);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateUsedColumn(int profileId, boolean used) {
    SqlSession session = mybatis.openSession();
    try {
      session.getMapper(QualityProfileMapper.class).updatedUsedColumn(profileId, used);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
