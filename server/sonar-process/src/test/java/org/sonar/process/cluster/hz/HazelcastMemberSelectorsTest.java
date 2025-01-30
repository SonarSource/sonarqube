/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MemberSelector;
import java.util.UUID;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessId.APP;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.PROCESS_KEY;

public class HazelcastMemberSelectorsTest {

  @Test
  public void selecting_ce_nodes() {
    Member member = mock(Member.class);
    MemberSelector underTest = HazelcastMemberSelectors.selectorForProcessIds(COMPUTE_ENGINE);

    when(member.getAttribute(PROCESS_KEY.getKey())).thenReturn(COMPUTE_ENGINE.getKey());
    assertThat(underTest.select(member)).isTrue();

    when(member.getAttribute(PROCESS_KEY.getKey())).thenReturn(WEB_SERVER.getKey());
    assertThat(underTest.select(member)).isFalse();

    when(member.getAttribute(PROCESS_KEY.getKey())).thenReturn(APP.getKey());
    assertThat(underTest.select(member)).isFalse();
  }

  @Test
  public void selecting_web_and_app_nodes() {
    Member member = mock(Member.class);
    MemberSelector underTest = HazelcastMemberSelectors.selectorForProcessIds(WEB_SERVER, APP);

    when(member.getAttribute(PROCESS_KEY.getKey())).thenReturn(COMPUTE_ENGINE.getKey());
    assertThat(underTest.select(member)).isFalse();

    when(member.getAttribute(PROCESS_KEY.getKey())).thenReturn(WEB_SERVER.getKey());
    assertThat(underTest.select(member)).isTrue();

    when(member.getAttribute(PROCESS_KEY.getKey())).thenReturn(APP.getKey());
    assertThat(underTest.select(member)).isTrue();
  }

  @Test
  public void selectorForMember_whenUuidMatches_returnTrue() {
    Member member = mock();
    Member member2 = mock();
    UUID uuid1 = uuidFromInt(0);
    when(member.getUuid()).thenReturn(uuid1);
    when(member2.getUuid()).thenReturn(uuid1);

    MemberSelector underTest = HazelcastMemberSelectors.selectorForMember(member);
    boolean found = underTest.select(member2);

    assertThat(found).isTrue();
  }

  private UUID uuidFromInt(int digit) {
    return UUID.fromString("00000000-0000-0000-0000-00000000000" + digit);
  }

  @Test
  public void selectorForMember_whenUuidDoesntMatch_returnTrue() {
    Member member = mock();
    Member member2 = mock();
    UUID uuid1 = uuidFromInt(0);
    UUID uuid2 = uuidFromInt(1);
    when(member.getUuid()).thenReturn(uuid1);
    when(member2.getUuid()).thenReturn(uuid2);

    MemberSelector underTest = HazelcastMemberSelectors.selectorForMember(member);
    boolean found = underTest.select(member2);

    assertThat(found).isFalse();
  }
}
