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

package org.sonar.wsclient.qprofile.internal;

import org.sonar.wsclient.qprofile.QProfileResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultQProfileResult implements QProfileResult {

  private Map json;

  DefaultQProfileResult(Map json) {
    this.json = json;
  }

  @Override
  public List<String> infos() {
    List<String> infos = new ArrayList<String>();
    List<String> jsonInfos = (List<String>) json.get("infos");
    if (jsonInfos != null) {
      for (String info : jsonInfos) {
        infos.add(info);
      }
    }
    return infos;
  }

  @Override
  public List<String> warnings() {
    List<String> warnings = new ArrayList<String>();
    List<String> jsonWarnings = (List<String>) json.get("warnings");
    if (jsonWarnings != null) {
      for (String warning : jsonWarnings) {
        warnings.add(warning);
      }
    }
    return warnings;
  }

}
