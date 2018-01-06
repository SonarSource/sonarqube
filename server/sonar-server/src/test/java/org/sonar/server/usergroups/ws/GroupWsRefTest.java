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
package org.sonar.server.usergroups.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.usergroups.ws.GroupWsRef.fromName;

public class GroupWsRefTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_ref_by_id() {
    GroupWsRef ref = GroupWsRef.fromId(10);
    assertThat(ref.hasId()).isTrue();
    assertThat(ref.getId()).isEqualTo(10);
    assertThat(ref.isAnyone()).isFalse();
  }

  @Test
  public void test_ref_by_name() {
    GroupWsRef ref = fromName("ORG1", "the-group");
    assertThat(ref.hasId()).isFalse();
    assertThat(ref.getOrganizationKey()).isEqualTo("ORG1");
    assertThat(ref.getName()).isEqualTo("the-group");
    assertThat(ref.isAnyone()).isFalse();
  }

  @Test
  public void test_equals_and_hashCode() {
    GroupWsRef refId1 = GroupWsRef.fromId(10);
    GroupWsRef refId2 = GroupWsRef.fromId(11);
    assertThat(refId1.equals(refId1)).isTrue();
    assertThat(refId1.equals(GroupWsRef.fromId(10))).isTrue();
    assertThat(refId1.hashCode()).isEqualTo(GroupWsRef.fromId(10).hashCode());
    assertThat(refId1.equals(refId2)).isFalse();

    GroupWsRef refName1 = fromName("ORG1", "the-group");
    GroupWsRef refName2 = fromName("ORG1", "the-group2");
    GroupWsRef refName3 = fromName("ORG2", "the-group2");
    assertThat(refName1.equals(refName1)).isTrue();
    assertThat(refName1.equals(fromName("ORG1", "the-group"))).isTrue();
    assertThat(refName1.hashCode()).isEqualTo(fromName("ORG1", "the-group").hashCode());
    assertThat(refName1.equals(refName2)).isFalse();
    assertThat(refName2.equals(refName3)).isFalse();
  }

  @Test
  public void test_toString() {
    GroupWsRef refId = GroupWsRef.fromId(10);
    assertThat(refId.toString()).isEqualTo("GroupWsRef{id=10, organizationKey='null', name='null'}");
  }

  @Test
  public void reference_anyone_by_its_name() {
    GroupWsRef ref = GroupWsRef.fromName("my-org", "Anyone");
    assertThat(ref.getOrganizationKey()).isEqualTo("my-org");
    assertThat(ref.getName()).isEqualTo("Anyone");
    assertThat(ref.isAnyone()).isTrue();

    // case-insensitive
    ref = GroupWsRef.fromName("my-org", "anyone");
    assertThat(ref.getOrganizationKey()).isEqualTo("my-org");
    assertThat(ref.getName()).isEqualTo("anyone");
    assertThat(ref.isAnyone()).isTrue();
  }
}
