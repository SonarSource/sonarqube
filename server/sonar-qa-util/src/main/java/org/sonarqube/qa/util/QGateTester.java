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
package org.sonarqube.qa.util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualitygates.CreateRequest;
import org.sonarqube.ws.client.qualitygates.QualitygatesService;
import org.sonarqube.ws.client.qualitygates.SelectRequest;

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
    String json = session.wsClient().wsConnector().call(new GetRequest("api/qualitygates/list")).failIfNotSuccessful().content();
    ListResponse response = ListResponse.parse(json);
    response.getQualityGates().stream()
      .filter(qualityGate -> !qualityGate.getId().equals(response.getDefault()))
      .forEach(qualityGate -> session.wsClient().wsConnector().call(new PostRequest("api/qualitygates/destroy").setParam("id", qualityGate.getId())).failIfNotSuccessful());
  }

  public Qualitygates.CreateResponse generate() {
    int id = ID_GENERATOR.getAndIncrement();
    return session.wsClient().qualitygates().create(new CreateRequest().setName("QualityGate" + id));
  }

  public void associateProject(Qualitygates.CreateResponse qualityGate, Project project){
    service().select(new SelectRequest().setGateId(String.valueOf(qualityGate.getId())).setProjectKey(project.getKey()));
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
