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
package org.sonar.process.cluster.hz;

import com.hazelcast.cluster.Member;
import java.io.IOException;
import java.util.UUID;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.NODE_NAME;

public class DistributedAnswerTest {


  private final Member member = newMember(UUID.randomUUID());
  private final DistributedAnswer<String> underTest = new DistributedAnswer<>();

  @Test
  public void getMembers_return_all_members() {
    underTest.setAnswer(member, "foo");
    underTest.setTimedOut(newMember(UUID.randomUUID()));
    underTest.setFailed(newMember(UUID.randomUUID()), new IOException("BOOM"));

    assertThat(underTest.getMembers()).hasSize(3);
  }

  @Test
  public void test_call_with_unknown_member() {
    assertThat(underTest.getAnswer(member)).isEmpty();
    assertThat(underTest.hasTimedOut(member)).isFalse();
    assertThat(underTest.getFailed(member)).isEmpty();
  }

  @Test
  public void test_setAnswer() {
    underTest.setAnswer(member, "foo");

    assertThat(underTest.getAnswer(member)).hasValue("foo");
    assertThat(underTest.hasTimedOut(member)).isFalse();
    assertThat(underTest.getFailed(member)).isEmpty();
  }

  @Test
  public void test_setTimedOut() {
    underTest.setTimedOut(member);

    assertThat(underTest.getAnswer(member)).isEmpty();
    assertThat(underTest.hasTimedOut(member)).isTrue();
    assertThat(underTest.getFailed(member)).isEmpty();
  }

  @Test
  public void test_setFailed() {
    IOException e = new IOException();
    underTest.setFailed(member, e);

    assertThat(underTest.getAnswer(member)).isEmpty();
    assertThat(underTest.hasTimedOut(member)).isFalse();
    assertThat(underTest.getFailed(member)).hasValue(e);
  }

  @Test
  public void member_can_be_referenced_multiple_times() {
    underTest.setTimedOut(member);
    underTest.setAnswer(member, "foo");
    IOException exception = new IOException();
    underTest.setFailed(member, exception);

    assertThat(underTest.hasTimedOut(member)).isTrue();
    assertThat(underTest.getAnswer(member)).hasValue("foo");
    assertThat(underTest.getFailed(member)).hasValue(exception);
  }

  @Test
  public void propagateExceptions_does_nothing_if_no_members() {
    // no errors
    underTest.propagateExceptions();
  }

  @Test
  public void propagateExceptions_does_nothing_if_no_errors() {
    underTest.setAnswer(newMember(UUID.randomUUID()), "bar");

    // no errors
    underTest.propagateExceptions();
  }

  @Test
  public void propagateExceptions_throws_ISE_if_at_least_one_timeout() {
    UUID uuid = UUID.randomUUID();
    UUID otherUuid = UUID.randomUUID();

    underTest.setAnswer(newMember(uuid), "baz");
    underTest.setTimedOut(newMember(otherUuid));

    assertThatThrownBy(underTest::propagateExceptions)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Distributed cluster action timed out in cluster nodes " + otherUuid);
  }

  @Test
  public void propagateExceptions_throws_ISE_if_at_least_one_failure() {
    UUID foo = UUID.randomUUID();
    UUID bar = UUID.randomUUID();

    underTest.setAnswer(newMember(bar), "baz");
    underTest.setFailed(newMember(foo), new IOException("BOOM"));

    assertThatThrownBy(underTest::propagateExceptions)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Distributed cluster action in cluster nodes " + foo + " (other nodes may have timed out)");
  }

  private static Member newMember(UUID uuid) {
    Member member = mock(Member.class);
    when(member.getUuid()).thenReturn(uuid);
    when(member.getAttribute(NODE_NAME.getKey())).thenReturn(uuid.toString());
    return member;
  }
}
