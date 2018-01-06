/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import java.util.ArrayList;
import java.util.List;
import org.sonar.db.qualityprofile.QProfileDto;

public class QProfileResult {

  private List<String> warnings;
  private List<String> infos;

  private QProfileDto profile;

  private List<ActiveRuleChange> changes;

  public QProfileResult() {
    warnings = new ArrayList<>();
    infos = new ArrayList<>();
    changes = new ArrayList<>();
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

  public QProfileDto profile() {
    return profile;
  }

  public QProfileResult setProfile(QProfileDto profile) {
    this.profile = profile;
    return this;
  }

  public List<ActiveRuleChange> getChanges() {
    return changes;
  }

  public QProfileResult addChanges(List<ActiveRuleChange> changes) {
    this.changes.addAll(changes);
    return this;
  }

  public QProfileResult add(QProfileResult result) {
    warnings.addAll(result.warnings());
    infos.addAll(result.infos());
    changes.addAll(result.getChanges());
    return this;
  }

}
