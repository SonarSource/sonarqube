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
import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Map;

public class QProfiles implements ServerComponent {

  private static final String LANGUAGE_PARAM = "language";

  private final QProfileProjectOperations projectOperations;
  private final QProfileProjectLookup projectLookup;
  private final QProfileLookup profileLookup;
  private final QProfileOperations operations;

  public QProfiles(QProfileProjectOperations projectOperations, QProfileProjectLookup projectLookup,
                   QProfileLookup profileLookup, QProfileOperations operations) {
    this.projectOperations = projectOperations;
    this.projectLookup = projectLookup;
    this.profileLookup = profileLookup;
    this.operations = operations;
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
  public QProfile defaultProfile(String language) {
    return profileLookup.defaultProfile(language);
  }

  public QProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin) {
    checkProfileNameParam(name);
    Validation.checkMandatoryParameter(language, LANGUAGE_PARAM);
    return operations.newProfile(name, language, xmlProfilesByPlugin, UserSession.get());
  }

  public void renameProfile(int profileId, String newName) {
    checkProfileNameParam(newName);
    operations.renameProfile(profileId, newName, UserSession.get());
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
   * Used in /project/profile
   */
  @CheckForNull
  public QProfile findProfileByProjectAndLanguage(long projectId, String language) {
    return projectLookup.findProfileByProjectAndLanguage(projectId, language);
  }

  public void addProject(int profileId, long projectId) {
    projectOperations.addProject(profileId, projectId, UserSession.get());
  }

  public void removeProject(int profileId, long projectId) {
    projectOperations.removeProject(profileId, projectId, UserSession.get());
  }

  public void removeProjectByLanguage(String language, long projectId) {
    projectOperations.removeProject(language, projectId, UserSession.get());
  }

  public void removeAllProjects(int profileId) {
    projectOperations.removeAllProjects(profileId, UserSession.get());
  }


  // PROFILE RULES
  public long countProfileRules(QProfile profile) {
    // TODO
    return -1;
  }

  public long countOverridingProfileRules(QProfile profile) {
    // TODO
    return -1;
    //return rules.countProfileRules(ProfileRuleQuery.create(profile.id()).setInheritance(QProfileRule.OVERRIDES));
  }

  private void checkProfileNameParam(String name) {
    if (Strings.isNullOrEmpty(name)) {
      throw BadRequestException.ofL10n("quality_profiles.please_type_profile_name");
    }
  }

}
