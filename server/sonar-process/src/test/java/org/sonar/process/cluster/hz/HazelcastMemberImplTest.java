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
package org.sonar.process.cluster.hz;

import com.hazelcast.cluster.memberselector.MemberSelectors;
import com.hazelcast.core.Member;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;

public class HazelcastMemberImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  // use loopback for support of offline builds
  private static InetAddress loopback = InetAddress.getLoopbackAddress();
  private static HazelcastMember member1;
  private static HazelcastMember member2;
  private static HazelcastMember member3;

  @BeforeClass
  public static void setUp() {
    int port1 = NetworkUtilsImpl.INSTANCE.getNextAvailablePort(loopback);
    int port2 = NetworkUtilsImpl.INSTANCE.getNextAvailablePort(loopback);
    int port3 = NetworkUtilsImpl.INSTANCE.getNextAvailablePort(loopback);
    member1 = newHzMember(port1, port2, port3);
    member2 = newHzMember(port2, port1, port3);
    member3 = newHzMember(port3, port1, port2);
  }

  @AfterClass
  public static void tearDown() {
    member1.close();
    member2.close();
    member3.close();
  }

  @Test
  public void call_executes_query_on_members() throws Exception {
    SuccessfulDistributedCall.COUNTER.set(0L);
    DistributedCall<Long> call = new SuccessfulDistributedCall();

    DistributedAnswer<Long> answer = member1.call(call, MemberSelectors.DATA_MEMBER_SELECTOR, 30_000L);

    assertThat(answer.getMembers()).extracting(Member::getUuid).containsOnlyOnce(member1.getUuid(), member2.getUuid(), member3.getUuid());
    assertThat(extractAnswers(answer)).containsOnlyOnce(0L, 1L, 2L);
  }

  @Test
  public void timed_out_calls_do_not_break_other_answers() throws InterruptedException {
    // member 1 and 3 success, member 2 times-out
    TimedOutDistributedCall.COUNTER.set(0L);
    DistributedCall call = new TimedOutDistributedCall();
    DistributedAnswer<Long> answer = member1.call(call, MemberSelectors.DATA_MEMBER_SELECTOR, 2_000L);

    assertThat(extractAnswers(answer)).containsOnlyOnce(0L, 2L);

    assertThat(extractTimeOuts(answer)).containsExactlyInAnyOrder(false, false, true);
  }

  @Test
  public void failed_calls_do_not_break_other_answers() throws InterruptedException {
    // member 1 and 3 success, member 2 fails
    FailedDistributedCall.COUNTER.set(0L);
    DistributedCall call = new FailedDistributedCall();
    DistributedAnswer<Long> answer = member1.call(call, MemberSelectors.DATA_MEMBER_SELECTOR, 2_000L);

    // 2 successful answers
    assertThat(extractAnswers(answer)).containsOnlyOnce(0L, 2L);

    // 1 failure
    List<Exception> failures = extractFailures(answer);
    assertThat(failures).hasSize(1);
    assertThat(failures.get(0)).hasMessageContaining("BOOM");
  }

  private static HazelcastMember newHzMember(int port, int... otherPorts) {
    return new HazelcastMemberBuilder()
      .setProcessId(ProcessId.COMPUTE_ENGINE)
      .setNodeName("name" + port)
      .setPort(port)
      .setNetworkInterface(loopback.getHostAddress())
      .setMembers(Arrays.stream(otherPorts).mapToObj(p -> loopback.getHostAddress() + ":" + p).collect(Collectors.toList()))
      .build();
  }

  private static Set<Long> extractAnswers(DistributedAnswer<Long> answer) {
    return answer.getMembers().stream()
      .map(answer::getAnswer)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toSet());
  }

  private static List<Exception> extractFailures(DistributedAnswer<Long> answer) {
    return answer.getMembers().stream()
      .map(answer::getFailed)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  private static List<Boolean> extractTimeOuts(DistributedAnswer<Long> answer) {
    return answer.getMembers().stream()
      .map(answer::hasTimedOut)
      .collect(Collectors.toList());
  }
}
