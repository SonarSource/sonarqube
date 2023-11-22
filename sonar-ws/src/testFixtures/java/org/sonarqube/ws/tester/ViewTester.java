/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ce.ActivityStatusRequest;
import org.sonarqube.ws.client.projects.ProjectsService;
import org.sonarqube.ws.client.projects.SearchRequest;
import org.sonarqube.ws.client.views.CreateRequest;
import org.sonarqube.ws.client.views.DeleteRequest;
import org.sonarqube.ws.client.views.ProjectsRequest;
import org.sonarqube.ws.client.views.RefreshRequest;
import org.sonarqube.ws.client.views.ShowRequest;
import org.sonarqube.ws.client.views.ViewsService;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class ViewTester extends ExternalResource {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  ViewTester(TesterSession session) {
    this.session = session;
  }

  public ViewsService service() {
    return session.wsClient().views();
  }

  public void deleteAll() {
    ProjectsService service = session.wsClient().projects();
    service.search(new SearchRequest().setQualifiers(asList("VW"))).getComponentsList()
      .forEach(p -> {
        waitForCeQueueEmpty();
        session.wsClient().views().delete(new DeleteRequest().setKey(p.getKey()));
      });
    waitForCeQueueEmpty();

    org.sonarqube.ws.client.components.SearchRequest searchRequest = new org.sonarqube.ws.client.components.SearchRequest().setQualifiers(asList("VW", "SVW"));
    assertThat(session.wsClient().components().search(searchRequest).getComponentsList()).isEmpty();

    assertNotViewInDef();
  }

  @SafeVarargs
  public final CreateRequest generate(Consumer<CreateRequest>... populators) {
    String key = generateKey();
    CreateRequest request = new CreateRequest()
      .setKey(key)
      .setName("Name " + key)
      .setDescription("Description " + key);
    stream(populators).forEach(p -> p.accept(request));
    service().create(request);
    return request;
  }

  @SafeVarargs
  public final String createSubPortfolio(String parentKey, Consumer<CreateRequest>... populators) {
    String key = generateKey();
    CreateRequest request = new CreateRequest()
      .setParent(parentKey)
      .setKey(key)
      .setName("Sub view name " + key)
      .setDescription("Sub view description " + key);
    stream(populators).forEach(p -> p.accept(request));
    service().create(request);
    return request.getKey();
  }

  public void addProject(String viewKey, Project project) {
    addProject(viewKey, project.getKey());
  }

  public void addProject(String viewKey, String projectKey) {
    session.wsClient().wsConnector().call(
      new PostRequest("/api/views/add_project")
        .setParam("key", viewKey)
        .setParam("project", projectKey))
      .failIfNotSuccessful();
  }

  public void addProjectBranch(String viewKey, String projectKey, String branch) {
    session.wsClient().wsConnector().call(
      new PostRequest("/api/views/add_project_branch")
        .setParam("key", viewKey)
        .setParam("project", projectKey)
        .setParam("branch", branch))
      .failIfNotSuccessful();
  }

  public AddLocalReferenceResponse addPortfolio(String key, String portfolioRefKey) {
    return AddLocalReferenceResponse.parse(session.wsClient().wsConnector().call(
        new PostRequest("/api/views/add_portfolio")
          .setParam("portfolio", key)
          .setParam("reference", portfolioRefKey))
      .failIfNotSuccessful().content());
  }

  public MoveResponse move(String key, String destinationKey) {
    return MoveResponse.parse(session.wsClient().wsConnector().call(
      new PostRequest("/api/views/move")
        .setParam("key", key)
        .setParam("destination", destinationKey))
      .failIfNotSuccessful().content());
  }

  public String generateKey() {
    int id = ID_GENERATOR.getAndIncrement();
    return "viewKey" + id;
  }

  public void refresh() {
    service().refresh(new RefreshRequest());
    waitForCeQueueEmpty();
  }

  public ViewTester waitForCeQueueEmpty() {
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

  public ListResponse list() {
    return ListResponse.parse(service().list());
  }

  public ShowResponse show(ShowRequest showRequest) {
    return ShowResponse.parse(service().show(showRequest));
  }

  public ProjectsResponse projects(ProjectsRequest request) {
    return ProjectsResponse.parse(service().projects(request));
  }

  public SearchResponse search(org.sonarqube.ws.client.views.SearchRequest searchRequest) {
    return SearchResponse.parse(service().search(searchRequest));
  }

  private void assertNotViewInDef() {
    assertThat(ListResponse.parse(service().list()).getViews()).isEmpty();
  }

  public static class ShowResponse {
    private final String key;
    private final String name;
    private final String desc;
    private final String qualifier;
    private final String selectionMode;
    private final String regexp;
    private final List<SelectedProject> selectedProjects;
    private final List<SubView> subViews;
    private final List<String> tags;

    public ShowResponse(String key, String name, String desc, String qualifier, String selectionMode, String regexp,
      List<SelectedProject> selectedProjects, @Nullable List<SubView> subViews, List<String> tags) {
      this.key = key;
      this.name = name;
      this.desc = desc;
      this.qualifier = qualifier;
      this.selectionMode = selectionMode;
      this.regexp = regexp;
      this.selectedProjects = selectedProjects;
      this.subViews = subViews;
      this.tags = tags;
    }

    public String getKey() {
      return key;
    }

    public String getName() {
      return name;
    }

    public String getDesc() {
      return desc;
    }

    public String getQualifier() {
      return qualifier;
    }

    public List<SelectedProject> getSelectedProjects() {
      return selectedProjects;
    }

    public String getSelectionMode() {
      return selectionMode;
    }

    public String getRegexp() {
      return regexp;
    }

    public List<String> getTags() {
      return tags;
    }

    @CheckForNull
    public List<SubView> getSubViews() {
      return subViews;
    }

    public static ShowResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ShowResponse.class);
    }

    public static class SelectedProject {
      private final String projectKey;
      private final List<String> selectedBranches;

      public SelectedProject(String projectKey, List<String> selectedBranches) {
        this.projectKey = projectKey;
        this.selectedBranches = selectedBranches;
      }

      public String getProjectKey() {
        return projectKey;
      }

      public List<String> getSelectedBranches() {
        return selectedBranches;
      }
    }

    public static class SubView {
      private final String key;
      private final String name;
      private final String desc;
      private final String selectionMode;
      private final String originalKey;
      private final String manual_measure_key;
      private final String manual_measure_value;
      private final List<SubView> subViews;

      public SubView(String key, String name, String desc, String selectionMode, String originalKey, String manual_measure_key, String manual_measure_value,
        List<SubView> subViews) {
        this.key = key;
        this.name = name;
        this.desc = desc;
        this.selectionMode = selectionMode;
        this.originalKey = originalKey;
        this.manual_measure_key = manual_measure_key;
        this.manual_measure_value = manual_measure_value;
        this.subViews = subViews;
      }

      public String getKey() {
        return key;
      }

      public String getName() {
        return name;
      }

      public String getDesc() {
        return desc;
      }

      public String getSelectionMode() {
        return selectionMode;
      }

      public String getOriginalKey() {
        return originalKey;
      }

      public String getManual_measure_key() {
        return manual_measure_key;
      }

      public String getManual_measure_value() {
        return manual_measure_value;
      }

      public List<SubView> getSubViews() {
        return subViews;
      }
    }
  }

  public static class ListResponse {

    private List<View> views;

    private ListResponse(List<View> views) {
      this.views = views;
    }

    public List<View> getViews() {
      return views;
    }

    public static ListResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ListResponse.class);
    }

    public static class View {
      private final String key;
      private final String name;
      private final String qualifier;

      private View(String key, String name, String qualifier) {
        this.key = key;
        this.name = name;
        this.qualifier = qualifier;
      }

      public String getKey() {
        return key;
      }

      public String getName() {
        return name;
      }

      public String getQualifier() {
        return qualifier;
      }

      public static View parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, View.class);
      }
    }
  }

  public static class AddLocalReferenceResponse {
    private final String key;
    private final String name;

    public AddLocalReferenceResponse(String key, String name) {
      this.key = key;
      this.name = name;
    }

    public static AddLocalReferenceResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, AddLocalReferenceResponse.class);
    }

    public String getKey() {
      return key;
    }

    public String getName() {
      return name;
    }
  }

  public static class ProjectsResponse {

    private List<Project> results;
    private boolean more;

    public ProjectsResponse(List<Project> results, boolean more) {
      this.results = results;
      this.more = more;
    }

    public List<Project> getProjects() {
      return results;
    }

    public boolean isMore() {
      return more;
    }

    public static ProjectsResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ProjectsResponse.class);
    }

    public static class Project {
      private final String key;
      private final String name;
      private final boolean selected;
      private final boolean enabled;

      private Project(String key, String name, boolean selected, boolean enabled) {
        this.key = key;
        this.name = name;
        this.selected = selected;
        this.enabled = enabled;
      }

      public String getKey() {
        return key;
      }

      public String getName() {
        return name;
      }

      public boolean isSelected() {
        return selected;
      }

      public boolean isEnabled() {
        return enabled;
      }
    }
  }

  public static class SearchResponse {

    private List<Component> components;

    private SearchResponse(List<Component> components) {
      this.components = components;
    }

    public List<Component> getComponents() {
      return components;
    }

    public static SearchResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, SearchResponse.class);
    }

    public static class Component {
      private final String key;
      private final String name;
      private final String qualifier;

      private Component(String key, String name, String qualifier) {
        this.key = key;
        this.name = name;
        this.qualifier = qualifier;
      }

      public String getKey() {
        return key;
      }

      public String getName() {
        return name;
      }

      public String getQualifier() {
        return qualifier;
      }

      public static Component parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Component.class);
      }
    }
  }

  public static class MoveResponse {
    private final String key;
    private final String name;

    public MoveResponse(String key, String name) {
      this.key = key;
      this.name = name;
    }

    public static MoveResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, MoveResponse.class);
    }

    public String getKey() {
      return key;
    }

    public String getName() {
      return name;
    }
  }
}
