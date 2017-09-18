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
import java.io.IOException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DistributedAnswerTest {

  private Member member = newMember("member1");
  private DistributedAnswer underTest = new DistributedAnswer();

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
  }

  @Test
  public void test_setTimedOut() {
    underTest.setTimedOut(member);

    assertThat(underTest.getAnswer(member)).isEmpty();
    assertThat(underTest.hasTimedOut(member)).isTrue();
  }

  @Test
  public void test_setFailed() {
    IOException e = new IOException();
    underTest.setFailed(member, e);

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

  private static Member newMember(String uuid) {
    Member member = mock(Member.class);
    when(member.getUuid()).thenReturn(uuid);
    return member;
  }
}
