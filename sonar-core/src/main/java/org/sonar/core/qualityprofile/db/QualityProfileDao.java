/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public class QualityProfileDao implements ServerComponent {

  private final MyBatis mybatis;

  public QualityProfileDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<QualityProfileDto> selectAll() {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectAll();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectDefaultProfile(String language, String key) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectDefaultProfile(language, key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectByProject(Long projectId, String propKeyPrefix) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectByProject(projectId, propKeyPrefix);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectById(Integer id) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectById(id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectByNameAndLanguage(String name, String language) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectByNameAndLanguage(StringUtils.upperCase(name), language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String propertyKey, String propertyValue) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(QualityProfileMapper.class).selectProjects(propertyKey, propertyValue);
    } finally {
      MyBatis.closeQuietly(session);
    }
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

  public void update(QualityProfileDto dto) {
    SqlSession session = mybatis.openSession();
    try {
      session.getMapper(QualityProfileMapper.class).update(dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(Integer id) {
    SqlSession session = mybatis.openSession();
    try {
      session.getMapper(QualityProfileMapper.class).delete(id);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
