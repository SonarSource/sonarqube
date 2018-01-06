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
package org.sonarqube.qa.util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.client.qualitygates.CreateRequest;
import org.sonarqube.ws.client.qualitygates.DestroyRequest;
import org.sonarqube.ws.client.qualitygates.ListRequest;
import org.sonarqube.ws.client.qualitygates.QualitygatesService;
import org.sonarqube.ws.client.qualitygates.SelectRequest;
import org.sonarqube.ws.client.qualitygates.SetAsDefaultRequest;

import static java.util.Arrays.stream;
import static org.sonarqube.ws.Qualitygates.CreateResponse;
import static org.sonarqube.ws.Qualitygates.ListWsResponse;

public class QGateTester {
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  QGateTester(TesterSession session) {
    this.session = session;
  }

  public QualitygatesService service() {
    return session.wsClient().qualitygates();
  }

  void deleteAll() {
    List<ListWsResponse.QualityGate> builtInQualityGates = session.wsClient().qualitygates().list(new ListRequest()).getQualitygatesList().stream()
      .filter(ListWsResponse.QualityGate::getIsBuiltIn)
      .collect(Collectors.toList());
    if (builtInQualityGates.size() == 1) {
      session.wsClient().qualitygates().setAsDefault(new SetAsDefaultRequest().setId(Long.toString(builtInQualityGates.get(0).getId())));
    }
    session.wsClient().qualitygates().list(new ListRequest()).getQualitygatesList().stream()
      .filter(qualityGate -> !qualityGate.getIsDefault())
      .filter(qualityGate -> !qualityGate.getIsBuiltIn())
      .forEach(qualityGate -> session.wsClient().qualitygates().destroy(new DestroyRequest().setId(Long.toString(qualityGate.getId()))));
  }

  public CreateResponse generate() {
    return generate(null);
  }

  @SafeVarargs
  public final CreateResponse generate(@Nullable Organizations.Organization organization, Consumer<CreateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest request = new CreateRequest()
      .setName("QualityGate " + id)
      .setOrganization(organization != null ? organization.getKey() : null);
    stream(populators).forEach(p -> p.accept(request));
    return session.wsClient().qualitygates().create(request);
  }

  public void associateProject(CreateResponse qualityGate, Project project) {
    associateProject(null, qualityGate, project);
  }

  public void associateProject(@Nullable Organizations.Organization organization, CreateResponse qualityGate, Project project) {
    service().select(new SelectRequest()
      .setOrganization(organization != null ? organization.getKey() : null)
      .setGateId(String.valueOf(qualityGate.getId()))
      .setProjectKey(project.getKey()));
  }

  public static class ListResponse {

    @SerializedName("default")
    private final String defaultQGate;
    @SerializedName("qualitygates")
    private final List<QGate> qualityGates = new ArrayList<>();

    public ListResponse(String defaultQGate) {
      this.defaultQGate = defaultQGate;
    }

    public List<QGate> getQualityGates() {
      return qualityGates;
    }

    public String getDefault() {
      return defaultQGate;
    }

    public static ListResponse parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ListResponse.class);
    }

    public static class QGate {
      @SerializedName("id")
      private final String id;
      @SerializedName("name")
      private final String name;

      public QGate(String id, String name) {
        this.id = id;
        this.name = name;
      }

      public String getId() {
        return id;
      }

      public String getName() {
        return name;
      }
    }
  }
}
