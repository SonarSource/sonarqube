/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Collection;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;

@FunctionalInterface
public interface DescendantProfilesSupplier {

  Result get(Collection<QProfileDto> profiles, Collection<Integer> ruleIds);

  final class Result {
    private final Collection<QProfileDto> profiles;
    private final Collection<ActiveRuleDto> activeRules;
    private final Collection<ActiveRuleParamDto> activeRuleParams;

    public Result(Collection<QProfileDto> profiles, Collection<ActiveRuleDto> activeRules, Collection<ActiveRuleParamDto> activeRuleParams) {
      this.profiles = profiles;
      this.activeRules = activeRules;
      this.activeRuleParams = activeRuleParams;
    }

    public Collection<QProfileDto> getProfiles() {
      return profiles;
    }

    public Collection<ActiveRuleDto> getActiveRules() {
      return activeRules;
    }

    public Collection<ActiveRuleParamDto> getActiveRuleParams() {
      return activeRuleParams;
    }
  }
}
