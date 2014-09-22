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
package org.sonar.wsclient.qualitygate.internal;

import org.json.simple.JSONArray;

import org.sonar.wsclient.unmarshallers.JsonUtils;
import org.sonar.wsclient.qualitygate.QualityGate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sonar.wsclient.qualitygate.QualityGates;

public class DefaultQualityGates implements QualityGates {

  private Map<Long, QualityGate> qualityGates;

  private Long defaultId;

  @SuppressWarnings("unchecked")
  public DefaultQualityGates(Map<String, Object> json) {
    qualityGates = new LinkedHashMap<Long, QualityGate>();
    JSONArray gatesJson = JsonUtils.getArray(json, "qualitygates");
    if (gatesJson != null) {
      for (Object entry: gatesJson) {
        QualityGate qGate = new DefaultQualityGate((Map<String, String>) entry);
        qualityGates.put(qGate.id(), qGate);
      }
    }
    defaultId = JsonUtils.getLong(json, "default");
  }

  @Override
  public Collection<QualityGate> qualityGates() {
    return qualityGates.values();
  }

  @Override
  public QualityGate defaultGate() {
    return qualityGates.get(defaultId);
  }

}
