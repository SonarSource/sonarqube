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
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileProjectLookup implements ServerComponent {

  private final QualityProfileDao qualityProfileDao;

  public QProfileProjectLookup(QualityProfileDao qualityProfileDao) {
    this.qualityProfileDao = qualityProfileDao;
  }

  public QProfileProjects projects(QualityProfileDto qualityProfile) {
    List<ComponentDto> componentDtos = qualityProfileDao.selectProjects(qualityProfile.getName(), QProfileOperations.PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage());
    List<Component> projects = newArrayList(Iterables.transform(componentDtos, new Function<ComponentDto, Component>() {
      @Override
      public Component apply(ComponentDto dto) {
        return (Component) dto;
      }
    }));
    return new QProfileProjects(QProfile.from(qualityProfile), projects);
  }

  public int countProjects(QProfile profile) {
    return qualityProfileDao.countProjects(profile.name(), QProfileOperations.PROFILE_PROPERTY_PREFIX + profile.language());
  }

  public List<QProfile> profiles(long projectId) {
    List<QualityProfileDto> dtos = qualityProfileDao.selectByProject(projectId, QProfileOperations.PROFILE_PROPERTY_PREFIX + "%");
    return newArrayList(Iterables.transform(dtos, new Function<QualityProfileDto, QProfile>() {
      @Override
      public QProfile apply(QualityProfileDto dto) {
        return QProfile.from(dto);
      }
    }));
  }

  public static class QProfileProjects {

    private QProfile profile;
    private List<Component> projects;

    public QProfileProjects(QProfile profile, List<Component> projects) {
      this.profile = profile;
      this.projects = projects;
    }

    public QProfile profile() {
      return profile;
    }

    public List<Component> projects() {
      return projects;
    }
  }
}
