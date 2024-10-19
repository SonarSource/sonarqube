/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarqube.ws.tester;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.applications.ApplicationsService;
import org.sonarqube.ws.client.applications.CreateRequest;
import org.sonarqube.ws.client.applications.DeleteRequest;
import org.sonarqube.ws.client.applications.SearchProjectsRequest;
import org.sonarqube.ws.client.applications.ShowRequest;
import org.sonarqube.ws.client.applications.UpdateRequest;
import org.sonarqube.ws.client.ce.ActivityStatusRequest;
import org.sonarqube.ws.client.projects.ProjectsService;
import org.sonarqube.ws.client.projects.SearchRequest;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationTester extends ExternalResource {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  ApplicationTester(TesterSession session) {
    this.session = session;
  }

  public ApplicationsService service() {
    return session.wsClient().applications();
  }

  public void deleteAll() {
    ProjectsService service = session.wsClient().projects();
    service.search(new SearchRequest().setQualifiers(singletonList("APP"))).getComponentsList()
      .forEach(p -> {
        waitForCeQueueEmpty();
        session.wsClient().applications().delete(new DeleteRequest().setApplication(p.getKey()));
      });
    waitForCeQueueEmpty();

    org.sonarqube.ws.client.components.SearchRequest searchRequest = new org.sonarqube.ws.client.components.SearchRequest().setQualifiers(singletonList("APP"));
    assertThat(session.wsClient().components().search(searchRequest).getComponentsList()).isEmpty();
  }

  public void updateName(String applicationKey, String name) {
    service().update(new UpdateRequest().setApplication(applicationKey).setName(name));
  }

  public ApplicationTester waitForCeQueueEmpty() {
    Ce.ActivityStatusWsResponse status;
    boolean empty;
    do {
      status = session.wsClient().ce().activityStatus(new ActivityStatusRequest());
      empty = status.getInProgress() + status.getPending() == 0;
      if (!empty) {
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      }

    } while (!empty);

    return this;
  }

  @SafeVarargs
  public final Application generate(Consumer<CreateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest request = new CreateRequest()
      .setKey("applicationKey" + id)
      .setName("applicationName" + id)
      .setDescription("applicationDescription" + id);
    stream(populators).forEach(p -> p.accept(request));
    return CreateResponse.parse(session.wsClient().applications().create(request)).getApplication();
  }

  public ShowResponse show(ShowRequest showRequest) {
    return ShowResponse.parse(session.wsClient().applications().show(showRequest));
  }

  public void refresh() {
    session.wsClient().wsConnector().call(new PostRequest("/api/applications/refresh")).failIfNotSuccessful();
    waitForCeQueueEmpty();
  }

  public SearchProjectsResponse searchProjects(SearchProjectsRequest searchProjectsRequest) {
    return SearchProjectsResponse.parse(session.wsClient().applications().searchProjects(searchProjectsRequest));
  }

  public static class CreateResponse {
    private final Application application;

    public CreateResponse(Application application) {
      this.application = application;
    }

    public Application getApplication() {
      return application;
    }

    public static CreateResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, CreateResponse.class);
    }
  }

  public static class ShowResponse {
    private final Application application;

    public ShowResponse(Application application) {
      this.application = application;
    }

    public Application getApplication() {
      return application;
    }

    public static ShowResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ShowResponse.class);
    }
  }

  public static class SearchProjectsResponse {
    private final Paging paging;
    private final List<Project> projects;

    public SearchProjectsResponse(Paging paging, List<Project> projects) {
      this.paging = paging;
      this.projects = projects;
    }

    public Paging getPaging() {
      return paging;
    }

    public List<Project> getProjects() {
      return projects;
    }

    public static SearchProjectsResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, SearchProjectsResponse.class);
    }

    public static class Project {
      private final String key;
      private final String name;
      private final boolean enabled;
      private final boolean selected;

      public Project(String key, String name, boolean enabled, boolean selected) {
        this.key = key;
        this.name = name;
        this.enabled = enabled;
        this.selected = selected;
      }

      public String getKey() {
        return key;
      }

      public String getName() {
        return name;
      }

      public boolean isEnabled() {
        return enabled;
      }

      public boolean isSelected() {
        return selected;
      }
    }
  }

  public static class Paging {
    public final int pageIndex;
    public final int pageSize;
    public final int total;

    public Paging(int pageIndex, int pageSize, int total) {
      this.pageIndex = pageIndex;
      this.pageSize = pageSize;
      this.total = total;
    }

    public int getPageIndex() {
      return pageIndex;
    }

    public int getPageSize() {
      return pageSize;
    }

    public int getTotal() {
      return total;
    }
  }

  public static class Application {
    private final String key;
    private final String branch;
    private final boolean isMain;
    private final String name;
    private final String description;
    private final String visibility;
    private final List<Project> projects;
    private final List<Application.Branch> branches;
    private final List<String> tags;

    public Application(String key, String branch, boolean isMain, String name, String description, String visibility, List<Project> projects, List<Branch> branches,
      List<String> tags) {
      this.key = key;
      this.branch = branch;
      this.isMain = isMain;
      this.name = name;
      this.description = description;
      this.visibility = visibility;
      this.projects = projects;
      this.branches = branches;
      this.tags = tags;
    }

    public String getKey() {
      return key;
    }

    public String getBranch() {
      return branch;
    }

    public boolean isMain() {
      return isMain;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public String getVisibility() {
      return visibility;
    }

    public List<Application.Project> getProjects() {
      return projects;
    }

    public List<String> getTags() {
      return tags;
    }

    @CheckForNull
    public List<Application.Branch> getBranches() {
      return branches;
    }

    public static class Project {
      private final String key;
      private final String branch;
      private final Boolean isMain;
      private final String name;
      private final boolean enabled;
      private final Boolean selected;

      public Project(String key, String branch, @Nullable Boolean isMain, String name, boolean enabled, @Nullable Boolean selected) {
        this.key = key;
        this.branch = branch;
        this.isMain = isMain;
        this.name = name;
        this.enabled = enabled;
        this.selected = selected;
      }

      public String getKey() {
        return key;
      }

      @CheckForNull
      public String getBranch() {
        return branch;
      }

      @CheckForNull
      public Boolean isMain() {
        return isMain;
      }

      public String getName() {
        return name;
      }

      public boolean isEnabled() {
        return enabled;
      }

      @CheckForNull
      public Boolean isSelected() {
        return selected;
      }
    }

    public static class Branch {
      private final String name;
      private final boolean isMain;

      public Branch(String name, boolean isMain) {
        this.name = name;
        this.isMain = isMain;
      }

      public String getName() {
        return name;
      }

      public boolean isMain() {
        return isMain;
      }
    }
  }

}
