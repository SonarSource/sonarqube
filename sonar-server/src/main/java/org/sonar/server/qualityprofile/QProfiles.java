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

import org.sonar.api.ServerComponent;

public class QProfiles implements ServerComponent {

  public void searchProfiles() {
    throw new UnsupportedOperationException();
  }

  public void searchProfile(String profile) {
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

  public void exportProfile(String profile, String language) {
    throw new UnsupportedOperationException();
  }

  public void exportProfile(String profile, String language, String plugin) {
    throw new UnsupportedOperationException();
  }

  public void restoreProfile() {
    throw new UnsupportedOperationException();
  }

  // INHERITANCE

  public void inheritance() {
    throw new UnsupportedOperationException();
  }

  public void inherit(String currentProfile, String parentProfile, String language) {
    throw new UnsupportedOperationException();
  }

  // CHANGELOG

  public void changelog(String profile, String language) {
    throw new UnsupportedOperationException();
  }

  // PROJECTS

  public void projects(String profile, String language) {
    throw new UnsupportedOperationException();
  }

  public void addProject(String projectKey, String profile, String language) {
    throw new UnsupportedOperationException();
  }

  public void removeProject(String projectKey, String profile, String language) {
    throw new UnsupportedOperationException();
  }

  public void removeAllProjects(String profile, String language) {
    throw new UnsupportedOperationException();
  }

  // ACTIVE RULES

  public void searchActiveRules() {
    throw new UnsupportedOperationException();
  }

  public void searchInactiveRules() {
    throw new UnsupportedOperationException();
  }

  public void activeRule() {
    throw new UnsupportedOperationException();
  }

  public void deactiveRule() {
    throw new UnsupportedOperationException();
  }

}
