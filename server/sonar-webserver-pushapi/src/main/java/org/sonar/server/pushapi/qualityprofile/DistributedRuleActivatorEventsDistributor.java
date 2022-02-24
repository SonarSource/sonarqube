/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.pushapi.qualityprofile;

import org.sonar.api.server.ServerSide;
import org.sonar.core.util.RuleActivationListener;
import org.sonar.core.util.RuleSetChangedEvent;
import org.sonar.process.cluster.hz.HazelcastMember;

@ServerSide
public class DistributedRuleActivatorEventsDistributor implements RuleActivatorEventsDistributor {

  private HazelcastMember hazelcastMember;

  public DistributedRuleActivatorEventsDistributor(HazelcastMember hazelcastMember) {
    this.hazelcastMember = hazelcastMember;
  }

  @Override
  public void subscribe(RuleActivationListener listener) {
    hazelcastMember.subscribeRuleActivationTopic(listener);
  }

  @Override
  public void pushEvent(RuleSetChangedEvent event) {
    hazelcastMember.publishEvent(event);
  }
}
