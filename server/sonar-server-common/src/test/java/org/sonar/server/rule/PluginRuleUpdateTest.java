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
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginRuleUpdateTest {

  @Test
  public void addOldImpact_whenOldImpactAdded_shouldContainOneImpact() {
    PluginRuleUpdate pluginRuleUpdate = new PluginRuleUpdate();

    pluginRuleUpdate.addOldImpact(SoftwareQuality.RELIABILITY, Severity.LOW);

    assertThat(pluginRuleUpdate.getOldImpacts()).hasSize(1);
  }

  @Test
  public void addNewImpact_whenNewImpactAdded_shouldContainOneImpact() {
    PluginRuleUpdate pluginRuleUpdate = new PluginRuleUpdate();

    pluginRuleUpdate.addNewImpact(SoftwareQuality.RELIABILITY, Severity.LOW);

    assertThat(pluginRuleUpdate.getNewImpacts()).hasSize(1);
  }

}
