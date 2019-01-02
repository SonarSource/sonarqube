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

import com.hazelcast.core.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.NODE_NAME;

/**
 * Answer of {@link DistributedCall}, aggregating the answers from
 * all the target members.
 */
public class DistributedAnswer<T> {

  private final Map<Member, T> answers = new HashMap<>();
  private final Set<Member> timedOutMembers = new HashSet<>();
  private final Map<Member, Exception> failedMembers = new HashMap<>();

  public Optional<T> getAnswer(Member member) {
    return Optional.ofNullable(answers.get(member));
  }

  public boolean hasTimedOut(Member member) {
    return timedOutMembers.contains(member);
  }

  public Optional<Exception> getFailed(Member member) {
    return Optional.ofNullable(failedMembers.get(member));
  }

  public Collection<Member> getMembers() {
    List<Member> members = new ArrayList<>();
    members.addAll(answers.keySet());
    members.addAll(timedOutMembers);
    members.addAll(failedMembers.keySet());
    return members;
  }

  public void setAnswer(Member member, T answer) {
    this.answers.put(member, answer);
  }

  public void setTimedOut(Member member) {
    this.timedOutMembers.add(member);
  }

  public void setFailed(Member member, Exception e) {
    failedMembers.put(member, e);
  }

  public void propagateExceptions() {
    if (!failedMembers.isEmpty()) {
      String failedMemberNames = failedMembers.keySet().stream()
        .map(m -> m.getStringAttribute(NODE_NAME.getKey()))
        .collect(Collectors.joining(", "));
      throw new IllegalStateException("Distributed cluster action in cluster nodes " + failedMemberNames + " (other nodes may have timed out)",
        failedMembers.values().iterator().next());
    }

    if (!timedOutMembers.isEmpty()) {
      String timedOutMemberNames = timedOutMembers.stream()
        .map(m -> m.getStringAttribute(NODE_NAME.getKey()))
        .collect(Collectors.joining(", "));
      throw new IllegalStateException("Distributed cluster action timed out in cluster nodes " + timedOutMemberNames);
    }
  }
}
