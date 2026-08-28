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
package org.sonar.server.permission.index;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.GithubWebhookUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.user.ServiceIdentity.AGENTIC_SHARED;
import static org.sonar.test.JsonAssert.assertJson;

public class WebAuthorizationTypeSupportTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WebAuthorizationTypeSupport underTest = new WebAuthorizationTypeSupport(userSession);

  @Test
  public void createQueryFilter_matches_every_document_with_an_authorization_parent_for_the_agentic_shared_service() {
    UserSession agenticSharedServiceSession = mock(UserSession.class);
    when(agenticSharedServiceSession.getServiceIdentity()).thenReturn(Optional.of(AGENTIC_SHARED));

    Query filter = new WebAuthorizationTypeSupport(agenticSharedServiceSession).createQueryFilterV2();

    assertThat(filter.isHasParent()).isTrue();
    assertThat(filter.hasParent().parentType()).isEqualTo("auth");
    assertThat(filter.hasParent().query().isMatchAll()).isTrue();
  }

  @Test
  public void createQueryFilter_does_not_match_every_document_for_another_service() {
    Query filter = new WebAuthorizationTypeSupport(new GithubWebhookUserSession()).createQueryFilterV2();

    assertThat(filter.isHasParent()).isTrue();
    assertThat(filter.hasParent().query().isMatchAll()).isFalse();
  }

  @Test
  public void createQueryFilter_sets_filter_on_anyone_group_if_user_is_anonymous() {
    userSession.anonymous();

    Query filter = underTest.createQueryFilterV2();

    assertThat(filter.isHasParent()).isTrue();
    assertJson(toJson(filter)).isSimilarTo("{" +
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
    UserDto userDto = UserTesting.newUserDto();
    userSession.logIn(userDto);

    Query filter = underTest.createQueryFilterV2();

    assertThat(filter.isHasParent()).isTrue();
    assertJson(toJson(filter)).isSimilarTo("{" +
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
      "                  \"auth_userIds\": {\"value\": \"" + userDto.getUuid() + "\"}" +
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
    GroupDto group1 = GroupTesting.newGroupDto().setUuid("10");
    GroupDto group2 = GroupTesting.newGroupDto().setUuid("11");
    UserDto userDto = UserTesting.newUserDto();
    userSession.logIn(userDto).setGroups(group1, group2);

    Query filter = underTest.createQueryFilterV2();

    assertThat(filter.isHasParent()).isTrue();
    assertJson(toJson(filter)).isSimilarTo("{" +
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
      "                  \"auth_userIds\": {\"value\": \"" + userDto.getUuid() + "\"}" +
      "                }" +
      "              }," +
      "              {" +
      "                \"term\": {" +
      "                  \"auth_groupIds\": {\"value\": \"10\"}" +
      "                }" +
      "              }," +
      "              {" +
      "                \"term\": {" +
      "                  \"auth_groupIds\": {\"value\": \"11\"}" +
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

  private static String toJson(JsonpSerializable serializable) {
    StringWriter writer = new StringWriter();
    JsonProvider provider = JsonProvider.provider();
    JsonGenerator generator = provider.createGenerator(writer);
    JacksonJsonpMapper mapper = new JacksonJsonpMapper();
    serializable.serialize(generator, mapper);
    generator.close();
    return writer.toString();
  }
}
