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

public class QProfileSearch implements ServerComponent {

  private final MyBatis myBatis;
  private final QualityProfileDao dao;

  public QProfileSearch(MyBatis myBatis, QualityProfileDao dao) {
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
  public QProfile defaultProfile(String language) {
    QualityProfileDto dto = dao.selectDefaultProfile(language, QProfileProjectService.PROPERTY_PREFIX + language);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

  public List<QProfile> children(QProfile profile) {
    return toQProfiles(dao.selectChildren(profile.name(), profile.language()));
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

  private void incrementAncestors(QProfile profile, List<QProfile> ancestors, SqlSession session){
    if (profile.parent() != null) {
      // TODO reuse same session
      QualityProfileDto parentDto = dao.selectParent(profile.id(), session);
      if (parentDto == null) {
        throw new IllegalStateException("Cannot find parent of profile : "+ profile.id());
      }
      QProfile parent = QProfile.from(parentDto);
      ancestors.add(parent);
      incrementAncestors(parent, ancestors, session);
    }
  }

  public int countChildren(QProfile profile) {
    return dao.countChildren(profile.name(), profile.language());
  }

  private List<QProfile> toQProfiles(List<QualityProfileDto> dtos){
    return newArrayList(Iterables.transform(dtos, new Function<QualityProfileDto, QProfile>() {
      @Override
      public QProfile apply(QualityProfileDto input) {
        return QProfile.from(input);
      }
    }));
  }
}
