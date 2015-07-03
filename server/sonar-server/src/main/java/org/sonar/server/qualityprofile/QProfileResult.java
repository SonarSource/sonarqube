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

import org.sonar.db.qualityprofile.QualityProfileDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileResult {

  private List<String> warnings;
  private List<String> infos;

  private QualityProfileDto profile;

  public QProfileResult() {
    warnings = newArrayList();
    infos = newArrayList();
  }

  public List<String> warnings() {
    return warnings;
  }

  public QProfileResult addWarnings(List<String> warnings) {
    this.warnings.addAll(warnings);
    return this;
  }

  public List<String> infos() {
    return infos;
  }

  public QProfileResult addInfos(List<String> infos) {
    this.infos.addAll(infos);
    return this;
  }

  public QualityProfileDto profile() {
    return profile;
  }

  public QProfileResult setProfile(QualityProfileDto profile) {
    this.profile = profile;
    return this;
  }

  public QProfileResult add(QProfileResult result) {
    warnings.addAll(result.warnings());
    infos.addAll(result.infos());
    return this;
  }

}
