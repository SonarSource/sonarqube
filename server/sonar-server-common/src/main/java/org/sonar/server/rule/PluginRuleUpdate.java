/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.rule;

import com.google.common.collect.Sets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;

/**
 * Represents a single update of a single rule done by new version of plugins at startup
 */
public class PluginRuleUpdate {

  private String ruleUuid;

  private CleanCodeAttribute newCleanCodeAttribute;
  private CleanCodeAttribute oldCleanCodeAttribute;
  private final Map<SoftwareQuality, Severity> newImpacts = new EnumMap<>(SoftwareQuality.class);
  private final Map<SoftwareQuality, Severity> oldImpacts = new EnumMap<>(SoftwareQuality.class);

  public String getRuleUuid() {
    return ruleUuid;
  }

  public void setRuleUuid(String ruleUuid) {
    this.ruleUuid = ruleUuid;
  }

  public void addOldImpact(SoftwareQuality softwareQuality, Severity severity) {
    oldImpacts.put(softwareQuality, severity);
  }

  public void addNewImpact(SoftwareQuality softwareQuality, Severity severity) {
    newImpacts.put(softwareQuality, severity);
  }

  public Map<SoftwareQuality, Severity> getNewImpacts() {
    return newImpacts;
  }

  public Map<SoftwareQuality, Severity> getOldImpacts() {
    return oldImpacts;
  }

  public Set<SoftwareQuality> getMatchingSoftwareQualities() {
    return Sets.intersection(newImpacts.keySet(), oldImpacts.keySet());
  }

  public CleanCodeAttribute getNewCleanCodeAttribute() {
    return newCleanCodeAttribute;
  }

  public void setNewCleanCodeAttribute(@Nullable CleanCodeAttribute newCleanCodeAttribute) {
    this.newCleanCodeAttribute = newCleanCodeAttribute;
  }

  public CleanCodeAttribute getOldCleanCodeAttribute() {
    return oldCleanCodeAttribute;
  }

  public void setOldCleanCodeAttribute(@Nullable CleanCodeAttribute oldCleanCodeAttribute) {
    this.oldCleanCodeAttribute = oldCleanCodeAttribute;
  }
}
