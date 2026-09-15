/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.notification;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class NotificationGroupSubscriptionsDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);
  private final DbSession dbSession = db.getSession();

  private final NotificationGroupSubscriptionsDao underTest = db.getDbClient().notificationGroupSubscriptionsDao();

  @Test
  void insert_and_selectAll() {
    GroupDto group = db.users().insertGroup();

    underTest.insert(dbSession, group.getUuid(), "TypeA", "ChannelX");
    dbSession.commit();

    List<NotificationGroupSubscriptionDto> results = underTest.selectAll(dbSession);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getGroupUuid()).isEqualTo(group.getUuid());
    assertThat(results.get(0).getNotificationType()).isEqualTo("TypeA");
    assertThat(results.get(0).getChannelKey()).isEqualTo("ChannelX");
  }

  @Nested
  class WithTwoGroupsAndMixedSubscriptions {
    GroupDto group1;
    GroupDto group2;

    @BeforeEach
    void setUp() {
      group1 = db.users().insertGroup();
      group2 = db.users().insertGroup();
      underTest.insert(dbSession, group1.getUuid(), "TypeA", "ChannelX");
      underTest.insert(dbSession, group1.getUuid(), "TypeB", "ChannelY");
      underTest.insert(dbSession, group2.getUuid(), "TypeA", "ChannelX");
      dbSession.commit();
    }

    @Test
    void selectByTypeAndChannel_returns_matching_subscriptions() {
      List<NotificationGroupSubscriptionDto> results = underTest.selectByTypeAndChannel(dbSession, "TypeA", "ChannelX");
      assertThat(results).hasSize(2);
      assertThat(results).extracting(NotificationGroupSubscriptionDto::getGroupUuid)
        .containsExactlyInAnyOrder(group1.getUuid(), group2.getUuid());
    }

    @Test
    void deleteByGroupUuid_removes_all_subscriptions_for_group() {
      int deleted = underTest.deleteByGroupUuid(dbSession, group1.getUuid());
      dbSession.commit();

      assertThat(deleted).isEqualTo(2);
      List<NotificationGroupSubscriptionDto> remaining = underTest.selectAll(dbSession);
      assertThat(remaining).hasSize(1);
      assertThat(remaining.get(0).getGroupUuid()).isEqualTo(group2.getUuid());
    }
  }

  @Test
  void deleteByGroupAndTypeAndChannel_removes_matching_row() {
    GroupDto group = db.users().insertGroup();

    underTest.insert(dbSession, group.getUuid(), "TypeA", "ChannelX");
    underTest.insert(dbSession, group.getUuid(), "TypeB", "ChannelX");
    dbSession.commit();

    int deleted = underTest.deleteByGroupAndTypeAndChannel(dbSession, group.getUuid(), "TypeA", "ChannelX");
    dbSession.commit();

    assertThat(deleted).isEqualTo(1);
    List<NotificationGroupSubscriptionDto> remaining = underTest.selectAll(dbSession);
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).getNotificationType()).isEqualTo("TypeB");
  }

  @Test
  void deleteByGroupAndTypeAndChannel_is_noop_when_not_found() {
    int deleted = underTest.deleteByGroupAndTypeAndChannel(dbSession, "unknown-uuid", "TypeA", "ChannelX");
    dbSession.commit();

    assertThat(deleted).isZero();
    assertThat(underTest.selectAll(dbSession)).isEmpty();
  }

  @Nested
  class SelectByUserUuidWithGroupName {

    private UserDto user;

    @BeforeEach
    void setUp() {
      user = db.users().insertUser();
    }

    @Test
    void returns_group_and_type_for_member() {
      GroupDto group = db.users().insertGroup("alpha");
      db.users().insertMember(group, user);
      underTest.insert(dbSession, group.getUuid(), "TypeA", "ChannelX");
      dbSession.commit();

      List<NotificationGroupSubscriptionDto> results = underTest.selectByUserUuidWithGroupName(dbSession, user.getUuid());

      assertThat(results)
        .extracting(NotificationGroupSubscriptionDto::getGroupName, NotificationGroupSubscriptionDto::getNotificationType)
        .containsExactly(tuple("alpha", "TypeA"));
    }

    @Test
    void returns_results_ordered_by_group_name_then_notification_type() {
      GroupDto groupBeta = db.users().insertGroup("beta");
      GroupDto groupAlpha = db.users().insertGroup("alpha");
      db.users().insertMember(groupBeta, user);
      db.users().insertMember(groupAlpha, user);
      underTest.insert(dbSession, groupBeta.getUuid(), "TypeB", "ChannelX");
      underTest.insert(dbSession, groupAlpha.getUuid(), "TypeZ", "ChannelX");
      underTest.insert(dbSession, groupAlpha.getUuid(), "TypeA", "ChannelX");
      dbSession.commit();

      List<NotificationGroupSubscriptionDto> results = underTest.selectByUserUuidWithGroupName(dbSession, user.getUuid());

      assertThat(results)
        .extracting(NotificationGroupSubscriptionDto::getGroupName, NotificationGroupSubscriptionDto::getNotificationType)
        .containsExactly(tuple("alpha", "TypeA"), tuple("alpha", "TypeZ"), tuple("beta", "TypeB"));
    }

    @Test
    void excludes_groups_user_is_not_member_of() {
      GroupDto memberGroup = db.users().insertGroup("member-group");
      GroupDto otherGroup = db.users().insertGroup("other-group");
      db.users().insertMember(memberGroup, user);
      underTest.insert(dbSession, memberGroup.getUuid(), "TypeA", "ChannelX");
      underTest.insert(dbSession, otherGroup.getUuid(), "TypeB", "ChannelX");
      dbSession.commit();

      List<NotificationGroupSubscriptionDto> results = underTest.selectByUserUuidWithGroupName(dbSession, user.getUuid());

      assertThat(results)
        .extracting(NotificationGroupSubscriptionDto::getGroupName, NotificationGroupSubscriptionDto::getNotificationType)
        .containsExactly(tuple("member-group", "TypeA"));
    }

    @Test
    void deduplicates_when_same_type_subscribed_on_multiple_channels() {
      GroupDto group = db.users().insertGroup("mygroup");
      db.users().insertMember(group, user);
      underTest.insert(dbSession, group.getUuid(), "TypeA", "ChannelX");
      underTest.insert(dbSession, group.getUuid(), "TypeA", "ChannelY");
      dbSession.commit();

      List<NotificationGroupSubscriptionDto> results = underTest.selectByUserUuidWithGroupName(dbSession, user.getUuid());

      assertThat(results)
        .extracting(NotificationGroupSubscriptionDto::getGroupName, NotificationGroupSubscriptionDto::getNotificationType)
        .containsExactly(tuple("mygroup", "TypeA"));
    }

    @Test
    void returns_empty_when_user_has_no_group_memberships() {
      GroupDto group = db.users().insertGroup("unused-group");
      underTest.insert(dbSession, group.getUuid(), "TypeA", "ChannelX");
      dbSession.commit();

      List<NotificationGroupSubscriptionDto> results = underTest.selectByUserUuidWithGroupName(dbSession, user.getUuid());

      assertThat(results).isEmpty();
    }
  }

}
