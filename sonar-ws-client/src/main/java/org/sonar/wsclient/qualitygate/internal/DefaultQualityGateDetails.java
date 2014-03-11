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

import org.sonar.wsclient.qualitygate.QualityGateCondition;
import org.sonar.wsclient.qualitygate.QualityGateDetails;

import java.util.Collection;
import java.util.Map;

public class DefaultQualityGateDetails extends DefaultQualityGate implements QualityGateDetails {

  private final Collection<QualityGateCondition> conditions;

  DefaultQualityGateDetails(Map<String, String> json, Collection<QualityGateCondition> conditions) {
    super(json);
    this.conditions = conditions;
  }

  @Override
  public Collection<QualityGateCondition> conditions() {
    return conditions;
  }

}
