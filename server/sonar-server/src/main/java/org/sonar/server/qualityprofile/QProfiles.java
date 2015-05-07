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

package org.sonar.server.qualityprofile;

import com.google.common.base.Strings;
import org.sonar.api.ServerSide;
import org.sonar.api.component.Component;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * Use {@link org.sonar.server.qualityprofile.QProfileService} instead
 */
@Deprecated
@ServerSide
public class QProfiles {

  private static final String LANGUAGE_PARAM = "language";

  private final QProfileProjectOperations projectOperations;
  private final QProfileProjectLookup projectLookup;
  private final QProfileLookup profileLookup;

  public QProfiles(QProfileProjectOperations projectOperations, QProfileProjectLookup projectLookup,
    QProfileLookup profileLookup) {
    this.projectOperations = projectOperations;
    this.projectLookup = projectLookup;
    this.profileLookup = profileLookup;
  }

  public List<QProfile> allProfiles() {
    return profileLookup.allProfiles();
  }

  public List<QProfile> profilesByLanguage(String language) {
    return profileLookup.profiles(language);
  }

  @CheckForNull
  public QProfile profile(int id) {
    return profileLookup.profile(id);
  }

  @CheckForNull
  public QProfile profile(String name, String language) {
    checkProfileNameParam(name);
    Validation.checkMandatoryParameter(language, LANGUAGE_PARAM);
    return profileLookup.profile(name, language);
  }

  @CheckForNull
  public QProfile parent(QProfile profile) {
    return profileLookup.parent(profile);
  }

  public List<QProfile> children(QProfile profile) {
    return profileLookup.children(profile);
  }

  public List<QProfile> ancestors(QProfile profile) {
    return profileLookup.ancestors(profile);
  }

  // PROJECTS

  public List<Component> projects(int profileId) {
    return projectLookup.projects(profileId);
  }

  public int countProjects(QProfile profile) {
    return projectLookup.countProjects(profile);
  }

  /**
   * Used in /project/profile and in /api/profiles
   */
  @CheckForNull
  public QProfile findProfileByProjectAndLanguage(long projectId, String language) {
    return projectLookup.findProfileByProjectAndLanguage(projectId, language);
  }

  public void addProject(String profileKey, String projectUuid) {
    projectOperations.addProject(profileKey, projectUuid, UserSession.get());
  }

  public void removeProject(String profileKey, String projectUuid) {
    projectOperations.removeProject(profileKey, projectUuid, UserSession.get());
  }

  public void removeProjectByLanguage(String language, long projectId) {
    projectOperations.removeProject(language, projectId, UserSession.get());
  }

  public void removeAllProjects(String profileKey) {
    projectOperations.removeAllProjects(profileKey, UserSession.get());
  }

  private void checkProfileNameParam(String name) {
    if (Strings.isNullOrEmpty(name)) {
      throw new BadRequestException("quality_profiles.please_type_profile_name");
    }
  }

}
