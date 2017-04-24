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
package org.sonar.server.organization.ws;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Organizations.SearchWsResponse;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionTest {
  private static final OrganizationDto ORGANIZATION_DTO = new OrganizationDto()
    .setUuid("a uuid")
    .setKey("the_key")
    .setName("the name")
    .setDescription("the description")
    .setUrl("the url")
    .setAvatarUrl("the avatar url")
    .setCreatedAt(1_999_000L)
    .setUpdatedAt(1_888_000L);
  private static final long SOME_DATE = 1_999_999L;

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2).setDisableDefaultOrganization(true);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SearchAction underTest = new SearchAction(dbTester.getDbClient(), new OrganizationsWsSupport(new OrganizationValidationImpl()));
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("search");
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isEqualTo("Search for organizations");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(3);
    assertThat(action.responseExample()).isEqualTo(getClass().getResource("search-example.json"));

    WebService.Param organizationsParam = action.param("organizations");
    assertThat(organizationsParam.isRequired()).isFalse();
    assertThat(organizationsParam.defaultValue()).isNull();
    assertThat(organizationsParam.description()).isEqualTo("Comma-separated list of organization keys");
    assertThat(organizationsParam.exampleValue()).isEqualTo("my-org-1,foocorp");
    assertThat(organizationsParam.since()).isEqualTo("6.3");
    WebService.Param pParam = action.param("p");
    assertThat(pParam.isRequired()).isFalse();
    assertThat(pParam.defaultValue()).isEqualTo("1");
    assertThat(pParam.description()).isEqualTo("1-based page number");
    WebService.Param psParam = action.param("ps");
    assertThat(psParam.isRequired()).isFalse();
    assertThat(psParam.defaultValue()).isEqualTo("25");
    assertThat(psParam.description()).isEqualTo("Page size. Must be greater than 0.");
  }

  @Test
  public void verify_response_example() throws URISyntaxException, IOException {
    when(system2.now()).thenReturn(SOME_DATE, SOME_DATE + 1000);
    insertOrganization(new OrganizationDto()
      .setUuid(Uuids.UUID_EXAMPLE_02)
      .setKey("bar-company")
      .setName("Bar Company")
      .setDescription("The Bar company produces quality software too.")
      .setUrl("https://www.bar.com")
      .setAvatarUrl("https://www.bar.com/logo.png")
      .setGuarded(false));
    insertOrganization(new OrganizationDto()
      .setUuid(Uuids.UUID_EXAMPLE_01)
      .setKey("foo-company")
      .setName("Foo Company")
      .setGuarded(true));

    String response = executeJsonRequest(null, null);

    assertJson(response).isSimilarTo(wsTester.getDef().responseExampleAsString());
  }

  @Test
  public void request_on_empty_db_returns_an_empty_organization_list() {
    assertThat(executeRequestAndReturnList(null, null)).isEmpty();
    assertThat(executeRequestAndReturnList(null, 1)).isEmpty();
    assertThat(executeRequestAndReturnList(1, null)).isEmpty();
    assertThat(executeRequestAndReturnList(1, 10)).isEmpty();
    assertThat(executeRequestAndReturnList(2, null)).isEmpty();
    assertThat(executeRequestAndReturnList(2, 1)).isEmpty();
  }

  @Test
  public void request_returns_empty_on_table_with_single_row_when_not_requesting_the_first_page() {
    when(system2.now()).thenReturn(SOME_DATE);
    insertOrganization(ORGANIZATION_DTO);

    assertThat(executeRequestAndReturnList(2, null)).isEmpty();
    assertThat(executeRequestAndReturnList(2, 1)).isEmpty();
    int somePage = Math.abs(new Random().nextInt(10)) + 2;
    assertThat(executeRequestAndReturnList(somePage, null)).isEmpty();
    assertThat(executeRequestAndReturnList(somePage, 1)).isEmpty();
  }

  @Test
  public void request_returns_rows_ordered_by_createdAt_descending_applying_requested_paging() {
    when(system2.now()).thenReturn(SOME_DATE, SOME_DATE + 1_000, SOME_DATE + 2_000, SOME_DATE + 3_000, SOME_DATE + 5_000);
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid3").setKey("key-3"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid1").setKey("key-1"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid2").setKey("key-2"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid5").setKey("key-5"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid4").setKey("key-4"));

    assertThat(executeRequestAndReturnList(1, 1))
      .extracting(Organization::getKey)
      .containsExactly("key-4");
    assertThat(executeRequestAndReturnList(2, 1))
      .extracting(Organization::getKey)
      .containsExactly("key-5");
    assertThat(executeRequestAndReturnList(3, 1))
      .extracting(Organization::getKey)
      .containsExactly("key-2");
    assertThat(executeRequestAndReturnList(4, 1))
      .extracting(Organization::getKey)
      .containsExactly("key-1");
    assertThat(executeRequestAndReturnList(5, 1))
      .extracting(Organization::getKey)
      .containsExactly("key-3");
    assertThat(executeRequestAndReturnList(6, 1))
      .isEmpty();

    assertThat(executeRequestAndReturnList(1, 5))
      .extracting(Organization::getKey)
      .containsExactly("key-4", "key-5", "key-2", "key-1", "key-3");
    assertThat(executeRequestAndReturnList(2, 5))
      .isEmpty();
    assertThat(executeRequestAndReturnList(1, 3))
      .extracting(Organization::getKey)
      .containsExactly("key-4", "key-5", "key-2");
    assertThat(executeRequestAndReturnList(2, 3))
      .extracting(Organization::getKey)
      .containsExactly("key-1", "key-3");
  }

  @Test
  public void request_returns_only_specified_keys_ordered_by_createdAt_when_filtering_keys() {
    when(system2.now()).thenReturn(SOME_DATE, SOME_DATE + 1_000, SOME_DATE + 2_000, SOME_DATE + 3_000, SOME_DATE + 5_000);
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid3").setKey("key-3"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid1").setKey("key-1"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid2").setKey("key-2"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid5").setKey("key-5"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid4").setKey("key-4"));

    assertThat(executeRequestAndReturnList(1, 10, "key-3", "key-1", "key-5"))
      .extracting(Organization::getKey)
      .containsExactly("key-5", "key-1", "key-3");
    // ensure order of arguments doesn't change order of result
    assertThat(executeRequestAndReturnList(1, 10, "key-1", "key-3", "key-5"))
      .extracting(Organization::getKey)
      .containsExactly("key-5", "key-1", "key-3");
  }

  @Test
  public void result_is_paginated() {
    when(system2.now()).thenReturn(SOME_DATE, SOME_DATE + 1_000, SOME_DATE + 2_000, SOME_DATE + 3_000, SOME_DATE + 5_000);
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid3").setKey("key-3"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid1").setKey("key-1"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid2").setKey("key-2"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid5").setKey("key-5"));
    insertOrganization(ORGANIZATION_DTO.setUuid("uuid4").setKey("key-4"));

    SearchWsResponse response = executeRequest(1, 1, "key-1", "key-3", "key-5");
    assertThat(response.getOrganizationsList()).extracting(Organization::getKey).containsOnly("key-5");
    assertThat(response.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsOnly(1, 1, 3);

    response = executeRequest(1, 2, "key-1", "key-3", "key-5");
    assertThat(response.getOrganizationsList()).extracting(Organization::getKey).containsOnly("key-5", "key-1");
    assertThat(response.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsOnly(1, 2, 3);

    response = executeRequest(2, 2, "key-1", "key-3", "key-5");
    assertThat(response.getOrganizationsList()).extracting(Organization::getKey).containsOnly("key-3");
    assertThat(response.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsOnly(2, 2, 3);

    response = executeRequest(null, null);
    assertThat(response.getOrganizationsList()).extracting(Organization::getKey).hasSize(5);
    assertThat(response.getPaging()).extracting(Paging::getPageIndex, Paging::getPageSize, Paging::getTotal).containsOnly(1, 25, 5);
  }

  @Test
  public void request_returns_empty_when_filtering_on_non_existing_key() {
    when(system2.now()).thenReturn(SOME_DATE);
    insertOrganization(ORGANIZATION_DTO);

    assertThat(executeRequestAndReturnList(1, 10, ORGANIZATION_DTO.getKey()))
      .extracting(Organization::getKey)
      .containsExactly(ORGANIZATION_DTO.getKey());
  }

  private List<Organization> executeRequestAndReturnList(@Nullable Integer page, @Nullable Integer pageSize, String... keys) {
    return executeRequest(page, pageSize, keys).getOrganizationsList();
  }

  private SearchWsResponse executeRequest(@Nullable Integer page, @Nullable Integer pageSize, String... keys) {
    TestRequest request = wsTester.newRequest();
    populateRequest(request, page, pageSize, keys);
    return request.executeProtobuf(SearchWsResponse.class);
  }

  private void insertOrganization(OrganizationDto dto) {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().organizationDao().insert(dbSession, dto, false);
    dbSession.commit();
  }

  private String executeJsonRequest(@Nullable Integer page, @Nullable Integer pageSize, String... keys) {
    TestRequest request = wsTester.newRequest()
      .setMediaType(MediaTypes.JSON);
    populateRequest(request, page, pageSize, keys);
    return request.execute().getInput();
  }

  private void populateRequest(TestRequest request, @Nullable Integer page, @Nullable Integer pageSize, String... keys) {
    if (keys.length > 0) {
      request.setParam("organizations", Joiner.on(',').join(Arrays.asList(keys)));
    }
    if (page != null) {
      request.setParam("p", valueOf(page));
    }
    if (pageSize != null) {
      request.setParam("ps", valueOf(pageSize));
    }
  }

}
