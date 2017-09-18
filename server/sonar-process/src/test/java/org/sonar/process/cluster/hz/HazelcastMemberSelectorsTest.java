/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import org.junit.Test;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HazelcastMemberSelectorsTest {

  @Test
  public void selecting_search_nodes_does_not_select_web_servers() throws Exception {
    Member member = mock(Member.class);
    when(member.getStringAttribute(HazelcastMember.Attribute.PROCESS_KEY)).thenReturn(ProcessId.WEB_SERVER.getKey());
    MemberSelector underTest = HazelcastMemberSelectors.selectorForProcessId(ProcessId.ELASTICSEARCH);

    boolean result = underTest.select(member);

    assertThat(result).isFalse();
  }

  @Test
  public void selecting_ce_nodes() throws Exception {
    Member member = mock(Member.class);
    when(member.getStringAttribute(HazelcastMember.Attribute.PROCESS_KEY)).thenReturn(ProcessId.COMPUTE_ENGINE.getKey());
    MemberSelector underTest = HazelcastMemberSelectors.selectorForProcessId(ProcessId.COMPUTE_ENGINE);

    boolean result = underTest.select(member);

    assertThat(result).isTrue();
  }
}