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
package org.sonar.process.cluster.hz;

import com.hazelcast.core.MemberSelector;
import java.util.List;
import org.sonar.process.ProcessId;

import static java.util.Arrays.asList;
import static org.sonar.process.ProcessId.fromKey;
import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.PROCESS_KEY;

public class HazelcastMemberSelectors {

  private HazelcastMemberSelectors() {
  }

  public static MemberSelector selectorForProcessIds(ProcessId... processIds) {
    List<ProcessId> processIdList = asList(processIds);
    return member -> {
      ProcessId memberProcessId = fromKey(member.getStringAttribute(PROCESS_KEY.getKey()));
      return processIdList.contains(memberProcessId);
    };
  }
}
