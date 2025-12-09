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
package org.sonar.server.usergroups.ws;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.usergroups.ws.GroupWsRef.fromName;

public class GroupWsRefTest {


  @Test
  public void test_ref_by_id() {
    GroupWsRef ref = GroupWsRef.fromUuid("10");
    assertThat(ref.hasUuid()).isTrue();
    assertThat(ref.getUuid()).isEqualTo("10");
    assertThat(ref.isAnyone()).isFalse();
  }

  @Test
  public void test_ref_by_name() {
    GroupWsRef ref = fromName("the-group");
    assertThat(ref.hasUuid()).isFalse();
    assertThat(ref.getName()).isEqualTo("the-group");
    assertThat(ref.isAnyone()).isFalse();
  }

  @Test
  public void test_equals_and_hashCode() {
    GroupWsRef refId1 = GroupWsRef.fromUuid("10");
    GroupWsRef refId2 = GroupWsRef.fromUuid("11");
    assertThat(refId1)
      .isEqualTo(refId1)
      .isEqualTo(GroupWsRef.fromUuid("10"))
      .hasSameHashCodeAs(GroupWsRef.fromUuid("10"))
      .isNotEqualTo(refId2);

    GroupWsRef refName1 = fromName("the-group");
    GroupWsRef refName2 = fromName("the-group2");
    GroupWsRef refName3 = fromName("the-group2");
    assertThat(refName1)
      .isEqualTo(refName1)
      .isEqualTo(fromName("the-group"))
      .hasSameHashCodeAs(fromName("the-group"))
      .isNotEqualTo(refName2);
    assertThat(refName2).isEqualTo(refName3);
  }

  @Test
  public void test_toString() {
    GroupWsRef refId = GroupWsRef.fromUuid("10");
    assertThat(refId).hasToString("GroupWsRef{uuid=10, name='null'}");
  }

  @Test
  public void reference_anyone_by_its_name() {
    GroupWsRef ref = GroupWsRef.fromName("Anyone");
    assertThat(ref.getName()).isEqualTo("Anyone");
    assertThat(ref.isAnyone()).isTrue();

    // case-insensitive
    ref = GroupWsRef.fromName("anyone");
    assertThat(ref.getName()).isEqualTo("anyone");
    assertThat(ref.isAnyone()).isTrue();
  }
}
