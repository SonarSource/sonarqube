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

package org.sonar.server.qualityprofile;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import javax.annotation.CheckForNull;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileLookup implements ServerComponent {

  private final MyBatis myBatis;
  private final QualityProfileDao dao;

  public QProfileLookup(MyBatis myBatis, QualityProfileDao dao) {
    this.myBatis = myBatis;
    this.dao = dao;
  }

  public List<QProfile> allProfiles() {
    return toQProfiles(dao.selectAll());
  }

  public List<QProfile> profiles(String language) {
    return toQProfiles(dao.selectByLanguage(language));
  }

  @CheckForNull
  public QProfile profile(int id) {
    SqlSession session = myBatis.openSession();
    try {
      return profile(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }


  @CheckForNull
  public QProfile profile(int id, SqlSession session) {
    QualityProfileDto dto = findQualityProfile(id, session);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

  @CheckForNull
  public QProfile profile(String name, String language) {
    QualityProfileDto dto = findQualityProfile(name, language);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

  @CheckForNull
  public QProfile defaultProfile(String language) {
    SqlSession session = myBatis.openSession();
    try {
      return defaultProfile(language, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  private QProfile defaultProfile(String language, SqlSession session) {
    QualityProfileDto dto = dao.selectDefaultProfile(language, QProfileOperations.PROFILE_PROPERTY_PREFIX + language, session);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

  @CheckForNull
  public QProfile parent(QProfile profile) {
    String parent = profile.parent();
    if (parent != null) {
      QualityProfileDto parentDto = findQualityProfile(parent, profile.language());
      if (parentDto != null) {
        return QProfile.from(parentDto);
      }
    }
    return null;
  }

  public List<QProfile> children(QProfile profile) {
    SqlSession session = myBatis.openSession();
    try {
      return children(profile, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<QProfile> children(QProfile profile, SqlSession session) {
    return toQProfiles(dao.selectChildren(profile.name(), profile.language(), session));
  }

  public List<QProfile> ancestors(QProfile profile) {
    List<QProfile> ancestors = newArrayList();
    SqlSession session = myBatis.openSession();
    try {
      incrementAncestors(profile, ancestors, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
    return ancestors;
  }

  public boolean isDeletable(QProfile profile, SqlSession session) {
    QProfile defaultProfile = defaultProfile(profile.language(), session);
    if (defaultProfile != null && (defaultProfile.id() == profile.id())) {
      return false;
    }
    return countChildren(profile, session) == 0;
  }

  public boolean isDeletable(QProfile profile) {
    SqlSession session = myBatis.openSession();
    try {
      return isDeletable(profile, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void incrementAncestors(QProfile profile, List<QProfile> ancestors, SqlSession session) {
    if (profile.parent() != null) {
      QualityProfileDto parentDto = dao.selectParent(profile.id(), session);
      if (parentDto == null) {
        throw new IllegalStateException("Cannot find parent of profile : " + profile.id());
      }
      QProfile parent = QProfile.from(parentDto);
      ancestors.add(parent);
      incrementAncestors(parent, ancestors, session);
    }
  }

  public int countChildren(QProfile profile) {
    SqlSession session = myBatis.openSession();
    try {
      return countChildren(profile, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countChildren(QProfile profile, SqlSession session) {
    return dao.countChildren(profile.name(), profile.language(), session);
  }

  private List<QProfile> toQProfiles(List<QualityProfileDto> dtos) {
    return newArrayList(Iterables.transform(dtos, new Function<QualityProfileDto, QProfile>() {
      @Override
      public QProfile apply(QualityProfileDto input) {
        return QProfile.from(input);
      }
    }));
  }

  @CheckForNull
  private QualityProfileDto findQualityProfile(int id, SqlSession session) {
    return dao.selectById(id, session);
  }

  @CheckForNull
  private QualityProfileDto findQualityProfile(String name, String language) {
    return dao.selectByNameAndLanguage(name, language);
  }

}
