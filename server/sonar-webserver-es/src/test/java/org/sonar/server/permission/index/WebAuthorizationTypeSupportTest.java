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
package org.sonar.server.permission.index;

import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;

public class WebAuthorizationTypeSupportTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WebAuthorizationTypeSupport underTest = new WebAuthorizationTypeSupport(userSession);

  @Test
  public void createQueryFilter_does_not_include_permission_filters_if_user_is_flagged_as_root() {
    userSession.logIn().setRoot();

    QueryBuilder filter = underTest.createQueryFilter();

    assertThat(filter).isInstanceOf(MatchAllQueryBuilder.class);
  }

  @Test
  public void createQueryFilter_sets_filter_on_anyone_group_if_user_is_anonymous() {
    userSession.anonymous();

    HasParentQueryBuilder filter = (HasParentQueryBuilder) underTest.createQueryFilter();

    assertJson(filter.toString()).isSimilarTo("{" +
        "  \"has_parent\" : {" +
        "    \"query\" : {" +
        "      \"bool\" : {" +
        "        \"filter\" : [{" +
        "          \"bool\" : {" +
        "            \"should\" : [{" +
        "              \"term\" : {" +
        "                \"auth_allowAnyone\" : {\"value\": true}" +
        "              }" +
        "            }]" +
        "          }" +
        "        }]" +
        "      }" +
        "    }," +
        "    \"parent_type\" : \"auth\"" +
        "  }" +
        "}");
  }

  @Test
  public void createQueryFilter_sets_filter_on_anyone_and_user_id_if_user_is_logged_in_but_has_no_groups() {
    userSession.logIn().setUserId(1234);

    HasParentQueryBuilder filter = (HasParentQueryBuilder) underTest.createQueryFilter();

    assertJson(filter.toString()).isSimilarTo("{" +
        "  \"has_parent\": {" +
        "    \"query\": {" +
        "      \"bool\": {" +
        "        \"filter\": [{" +
        "          \"bool\": {" +
        "            \"should\": [" +
        "              {" +
        "                \"term\": {" +
        "                  \"auth_allowAnyone\": {\"value\": true}" +
        "                }" +
        "              }," +
        "              {" +
        "                \"term\": {" +
        "                  \"auth_userIds\": {\"value\": 1234}" +
        "                }" +
        "              }" +
        "            ]" +
        "          }" +
        "        }]" +
        "      }" +
        "    }," +
        "    \"parent_type\": \"auth\"" +
        "  }" +
        "}");
  }

  @Test
  public void createQueryFilter_sets_filter_on_anyone_and_user_id_and_group_ids_if_user_is_logged_in_and_has_groups() {
    GroupDto group1 = GroupTesting.newGroupDto().setId(10);
    GroupDto group2 = GroupTesting.newGroupDto().setId(11);
    userSession.logIn().setUserId(1234).setGroups(group1, group2);

    HasParentQueryBuilder filter = (HasParentQueryBuilder) underTest.createQueryFilter();

    assertJson(filter.toString()).isSimilarTo("{" +
        "  \"has_parent\": {" +
        "    \"query\": {" +
        "      \"bool\": {" +
        "        \"filter\": [{" +
        "          \"bool\": {" +
        "            \"should\": [" +
        "              {" +
        "                \"term\": {" +
        "                  \"auth_allowAnyone\": {\"value\": true}" +
        "                }" +
        "              }," +
        "              {" +
        "                \"term\": {" +
        "                  \"auth_userIds\": {\"value\": 1234}" +
        "                }" +
        "              }," +
        "              {" +
        "                \"term\": {" +
        "                  \"auth_groupIds\": {\"value\": 10}" +
        "                }" +
        "              }," +
        "              {" +
        "                \"term\": {" +
        "                  \"auth_groupIds\": {\"value\": 11}" +
        "                }" +
        "              }" +
        "            ]" +
        "          }" +
        "        }]" +
        "      }" +
        "    }," +
        "    \"parent_type\": \"auth\"" +
        "  }" +
        "}");
  }
}
