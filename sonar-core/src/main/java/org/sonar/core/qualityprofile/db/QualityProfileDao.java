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

import com.google.common.base.Preconditions;
import org.sonar.api.ServerComponent;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;
import java.util.List;

public class QualityProfileDao implements ServerComponent, DaoComponent {

  private final MyBatis mybatis;

  public QualityProfileDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  @CheckForNull
  public QualityProfileDto getByKey(QualityProfileKey key, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectByNameAndLanguage(key.name(), key.lang());
  }

  public List<QualityProfileDto> findAll(DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectAll();
  }

  public void insert(DbSession session, QualityProfileDto profile, QualityProfileDto... otherProfiles) {
    QualityProfileMapper mapper = session.getMapper(QualityProfileMapper.class);
    doInsert(mapper, profile);
    for (QualityProfileDto other : otherProfiles) {
      doInsert(mapper, other);
    }
  }

  private void doInsert(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkArgument(profile.getId() == null, "Quality profile is already persisted (got id %d)", profile.getId());
    mapper.insert(profile);
  }

  /**
   * @deprecated use {@link #insert(org.sonar.core.persistence.DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void insert(QualityProfileDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(session, dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(DbSession session, QualityProfileDto profile, QualityProfileDto... otherProfiles) {
    QualityProfileMapper mapper = session.getMapper(QualityProfileMapper.class);
    doUpdate(mapper, profile);
    for (QualityProfileDto otherProfile : otherProfiles) {
      doUpdate(mapper, otherProfile);
    }
  }

  private void doUpdate(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkArgument(profile.getId() != null, "Quality profile is not persisted");
    mapper.update(profile);
  }

  /**
   * @deprecated use {@link #update(org.sonar.core.persistence.DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void update(QualityProfileDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      update(session, dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }


  public void delete(DbSession session, QualityProfileDto profile, QualityProfileDto... otherProfiles) {
    QualityProfileMapper mapper = session.getMapper(QualityProfileMapper.class);
    doDelete(mapper, profile);
    for (QualityProfileDto otherProfile : otherProfiles) {
      doDelete(mapper, otherProfile);
    }
  }

  private void doDelete(QualityProfileMapper mapper, QualityProfileDto profile) {
    Preconditions.checkNotNull(profile.getId(), "Quality profile is not persisted");
    mapper.delete(profile.getId());
  }

  /**
   * @deprecated use {@link #delete(org.sonar.core.persistence.DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void delete(int id, DbSession session) {
    session.getMapper(QualityProfileMapper.class).delete(id);
  }

  /**
   * @deprecated use {@link #delete(org.sonar.core.persistence.DbSession, QualityProfileDto, QualityProfileDto...)}
   */
  @Deprecated
  public void delete(int id) {
    DbSession session = mybatis.openSession(false);
    try {
      delete(id, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectAll() {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).selectAll();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectDefaultProfile(String language, String key, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectDefaultProfile(language, key);
  }

  public QualityProfileDto selectDefaultProfile(String language, String key) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectDefaultProfile(language, key, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectByProjectAndLanguage(long projectId, String language, String key) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).selectByProjectAndLanguage(projectId, language, key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectByLanguage(String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).selectByLanguage(language);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectById(int id, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectById(id);
  }

  @CheckForNull
  public QualityProfileDto selectById(int id) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public QualityProfileDto selectParent(int childId, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectParent(childId);
  }

  @CheckForNull
  public QualityProfileDto selectParent(int childId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParent(childId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QualityProfileDto> selectChildren(String name, String language, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectChildren(name, language);
  }

  public List<QualityProfileDto> selectChildren(String name, String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectChildren(name, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countChildren(String name, String language, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).countChildren(name, language);
  }

  public int countChildren(String name, String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return countChildren(name, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityProfileDto selectByNameAndLanguage(String name, String language, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectByNameAndLanguage(name, language);
  }

  public QualityProfileDto selectByNameAndLanguage(String name, String language) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByNameAndLanguage(name, language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String propertyKey, String propertyValue) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectProjects(propertyKey, propertyValue, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ComponentDto> selectProjects(String propertyKey, String propertyValue, DbSession session) {
    return session.getMapper(QualityProfileMapper.class).selectProjects(propertyKey, propertyValue);
  }

  public int countProjects(String propertyKey, String propertyValue) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(QualityProfileMapper.class).countProjects(propertyKey, propertyValue);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateUsedColumn(int profileId, boolean used) {
    DbSession session = mybatis.openSession(false);
    try {
      session.getMapper(QualityProfileMapper.class).updatedUsedColumn(profileId, used);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
