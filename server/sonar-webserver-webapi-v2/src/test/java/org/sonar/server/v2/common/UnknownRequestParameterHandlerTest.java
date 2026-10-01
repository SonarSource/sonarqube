/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either
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
package org.sonar.server.v2.common;

import org.junit.jupiter.api.Test;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.group.request.GroupsSearchRestRequest;
import org.sonar.server.v2.api.model.RestPage;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UnknownRequestParameterHandlerTest {

  private static final String GROUPS = "/authorizations/groups";

  private final MockMvc mockMvc = ControllerTester.getMockMvc(new GroupsController());

  @Test
  void preHandle_whenParameterIsUnknown_returnsBadRequest() throws Exception {
    mockMvc.perform(get(GROUPS).queryParam("maneged", "true"))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Parameter \\"maneged\\" is not a valid parameter for /authorizations/groups."}
        """));
  }

  @Test
  void preHandle_whenSeveralParametersAreUnknown_listsThemInOrder() throws Exception {
    mockMvc.perform(get(GROUPS).queryParam("maneged", "true").queryParam("foo", "1"))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Parameters \\"foo\\", \\"maneged\\" are not valid parameters for /authorizations/groups."}
        """));
  }

  @Test
  void preHandle_whenParameterNameHasABracketSuffix_returnsBadRequest() throws Exception {
    mockMvc.perform(get(GROUPS).queryParam("managed[0]", "true"))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Parameter \\"managed[0]\\" is not a valid parameter for /authorizations/groups."}
        """));
  }

  @Test
  void preHandle_whenQueryContainsACacheBuster_acceptsTheRequest() throws Exception {
    mockMvc.perform(get(GROUPS).queryParam("_", "1727700000").queryParam("managed", "true"))
      .andExpect(status().isOk());
  }

  @Test
  void preHandle_whenCacheBusterIsSentWithAnUnknownParameter_rejectsOnlyTheUnknownParameter() throws Exception {
    mockMvc.perform(get(GROUPS).queryParam("_", "1727700000").queryParam("maneged", "true"))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Parameter \\"maneged\\" is not a valid parameter for /authorizations/groups."}
        """));
  }

  @Test
  void preHandle_whenParameterObjectIsAClass_acceptsItsFields() throws Exception {
    mockMvc.perform(get("/class-search").queryParam("managed", "true").queryParam("pageIndex", "1"))
      .andExpect(status().isOk());
  }

  @Test
  void preHandle_whenParameterObjectIsAClassAndParameterIsUnknown_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/class-search").queryParam("maneged", "true"))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Parameter \\"maneged\\" is not a valid parameter for /class-search."}
        """));
  }

  @Test
  void preHandle_whenParametersAreDeclared_acceptsTheRequest() throws Exception {
    mockMvc.perform(get(GROUPS)
        .queryParam("managed", "true")
        .queryParam("q", "sonar")
        .queryParam("userId", "user")
        .queryParam("userId!", "excluded")
        .queryParam("pageSize", "10")
        .queryParam("pageIndex", "2"))
      .andExpect(status().isOk());
  }

  @Test
  void preHandle_whenJsonRequestHasAnUnknownQueryParameter_returnsBadRequest() throws Exception {
    mockMvc.perform(post(GROUPS)
        .contentType(APPLICATION_JSON_VALUE)
        .content("{}")
        .queryParam("maneged", "true"))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Parameter \\"maneged\\" is not a valid parameter for /authorizations/groups."}
        """));
  }

  @Test
  void preHandle_whenBodyIsFormUrlEncoded_doesNotRejectBodyFields() throws Exception {
    mockMvc.perform(post("/form")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .content("command=/sonar&text=hello"))
      .andExpect(status().isOk());
  }

  @Test
  void preHandle_whenFormUrlEncodedRequestHasAnUnknownQueryParameter_returnsBadRequest() throws Exception {
    mockMvc.perform(post("/form")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .content("command=/sonar&text=hello")
        .queryParam("maneged", "true"))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Parameter \\"maneged\\" is not a valid parameter for /form."}
        """));
  }

  @Test
  void preHandle_whenRequestHasNoQueryString_acceptsTheRequest() throws Exception {
    mockMvc.perform(get(GROUPS))
      .andExpect(status().isOk());
  }

  @Test
  void preHandle_whenQueryHasEmptySegmentsAndAValuelessName_acceptsDeclaredNames() throws Exception {
    mockMvc.perform(get(GROUPS).with(request -> {
      request.setQueryString("managed&&q=sonar&=ignored");
      return request;
    }))
      .andExpect(status().isOk());
  }

  @Test
  void preHandle_whenQueryStringIsNotPercentEncoded_returnsBadRequest() throws Exception {
    mockMvc.perform(get(GROUPS).with(request -> {
      request.setQueryString("%GG=1");
      return request;
    }))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("""
        {"message":"Query string for /authorizations/groups is not valid."}
        """));
  }

  @RequestMapping
  interface GroupsApi {
    @GetMapping(GROUPS)
    String search(
      @ParameterObject GroupsSearchRestRequest groupsSearch,
      @RequestParam(name = "userId!", required = false) String excludedUserId,
      @ParameterObject RestPage page);

    @PostMapping(path = GROUPS, consumes = APPLICATION_JSON_VALUE)
    String create(@RequestBody String body);

    @PostMapping(path = "/form", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    String form(@RequestBody String body);

    @GetMapping("/class-search")
    String classSearch(@ParameterObject ClassSearchRequest request);
  }

  @RestController
  static class GroupsController implements GroupsApi {
    @Override
    public String search(GroupsSearchRestRequest groupsSearch, String excludedUserId, RestPage page) {
      return "ok";
    }

    @Override
    public String create(String body) {
      return body;
    }

    @Override
    public String form(String body) {
      return body;
    }

    @Override
    public String classSearch(ClassSearchRequest request) {
      return request.getManaged() + request.getPageIndex();
    }
  }

  static class PageFields {
    private String pageIndex;

    String getPageIndex() {
      return pageIndex;
    }
  }

  static class ClassSearchRequest extends PageFields {
    private String managed;

    String getManaged() {
      return managed;
    }
  }

}
