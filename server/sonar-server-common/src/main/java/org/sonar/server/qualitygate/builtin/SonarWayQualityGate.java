/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualitygate.builtin;

import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.server.qualitygate.Condition;

import static java.util.Arrays.asList;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN;
import static org.sonar.server.qualitygate.Condition.Operator.LESS_THAN;

@ServerSide
public class SonarWayQualityGate implements BuiltInQualityGate {

  public static final String NAME = "Sonar way";

  private final List<Condition> conditions;

  public SonarWayQualityGate() {
    this.conditions = asList(
      new Condition(NEW_VIOLATIONS_KEY, GREATER_THAN, "0"),
      new Condition(NEW_COVERAGE_KEY, LESS_THAN, "80"),
      new Condition(NEW_DUPLICATED_LINES_DENSITY_KEY, GREATER_THAN, "3"),
      new Condition(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, LESS_THAN, "100"));
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean supportsAiCode() {
    return false;
  }

  @Override
  public List<Condition> getConditions() {
    return conditions;
  }
}
