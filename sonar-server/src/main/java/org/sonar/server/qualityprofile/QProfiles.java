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
import org.sonar.api.rule.RuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfiles implements ServerComponent {

  private final QualityProfileDao dao;

  public QProfiles(QualityProfileDao dao) {
    this.dao = dao;
  }

  public List<QProfile> searchProfiles() {
    List<QualityProfileDto> dtos = dao.selectAll();
    return newArrayList(Iterables.transform(dtos, new Function<QualityProfileDto, QProfile>() {
      @Override
      public QProfile apply(QualityProfileDto input) {
        return QProfile.from(input);
      }
    }));
  }

  public void searchProfile(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void newProfile() {
    throw new UnsupportedOperationException();
  }

  public void deleteProfile() {
    throw new UnsupportedOperationException();
  }

  public void renameProfile() {
    throw new UnsupportedOperationException();
  }

  public void setDefaultProfile() {
    throw new UnsupportedOperationException();
  }

  public void copyProfile() {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(QProfileKey profile, String plugin) {
    throw new UnsupportedOperationException();
  }

  public void restoreProfile() {
    throw new UnsupportedOperationException();
  }

  // INHERITANCE

  public void inheritance() {
    throw new UnsupportedOperationException();
  }

  public void inherit(QProfileKey currentProfile, QProfileKey parentProfile) {
    throw new UnsupportedOperationException();
  }

  // CHANGELOG

  public void changelog(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  // PROJECTS

  public void projects(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void addProject(QProfileKey profile, String projectKey) {
    throw new UnsupportedOperationException();
  }

  public void removeProject(QProfileKey profile, String projectKey) {
    throw new UnsupportedOperationException();
  }

  public void removeAllProjects(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  // ACTIVE RULES

  public void searchActiveRules(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void searchInactiveRules(QProfileKey profile) {
    throw new UnsupportedOperationException();
  }

  public void activeRule(QProfileKey profile, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

  public void deactiveRule(QProfileKey profile, RuleKey ruleKey) {
    throw new UnsupportedOperationException();
  }

}
