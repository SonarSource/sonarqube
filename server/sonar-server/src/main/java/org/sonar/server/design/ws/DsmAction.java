/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.design.ws;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.core.util.NonNullInputPredicate;
import org.sonar.server.db.DbClient;
import org.sonar.server.design.db.DsmDataEncoder;
import org.sonar.server.design.db.DsmDb;
import org.sonar.server.measure.ServerMetrics;
import org.sonar.server.user.UserSession;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class DsmAction implements DependenciesAction {

  private static final String PARAM_COMPONENT_UUID = "componentUuid";
  private static final String PARAM_DISPLAY_NO_DEP = "displayComponentsWithoutDep";

  private static Comparator<ComponentDto> COMPONENT_BY_NAME_COMPARATOR = new Comparator<ComponentDto>() {
    @Override
    public int compare(ComponentDto c1, ComponentDto c2) {
      return c1.name().compareTo(c2.name());
    }
  };

  private final DbClient dbClient;

  public DsmAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("dsm")
      .setDescription("Display the DSM")
      .setSince("5.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-dsm.json"));

    // TODO add component key param
    action.createParam(PARAM_COMPONENT_UUID)
      .setDescription("Component uuid (only sub project or directory are allowed for the moment)")
      .setRequired(true)
      .setExampleValue("2312cd03-b514-4acc-94f4-5c5e8e0062b2");

    action.createParam(PARAM_DISPLAY_NO_DEP)
      .setDescription("Display component with no dependencies or not")
      .setDefaultValue(false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String componentUuid = request.mandatoryParam(PARAM_COMPONENT_UUID);
    boolean displayComponentWithoutDep = request.mandatoryParamAsBoolean(PARAM_DISPLAY_NO_DEP);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getByUuid(session, componentUuid);
      UserSession.get().checkProjectUuidPermission(UserRole.USER, component.projectUuid());

      // TODO manage old dsm measure
      DsmDb.Data dsmData = loadDsmData(session, component.key());
      List<ComponentDto> components;
      if (!displayComponentWithoutDep) {
        components = dbClient.componentDao().getByUuids(session, dsmData.getUuidList());
      } else {
        components = dbClient.componentDao().selectChildrenByComponent(session, componentUuid);
      }

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      writeComponents(json, dsmData.getUuidList(), components);
      writeDependencies(json, dsmData.getCellList());
      json.endObject().close();

    } finally {
      session.close();
    }
  }

  private DsmDb.Data loadDsmData(DbSession session, String componentKey) {
    // TODO replace component key by uuid
    MeasureDto measureDto = dbClient.measureDao().findByComponentKeyAndMetricKey(session, componentKey, ServerMetrics.DEPENDENCY_MATRIX_KEY);
    if (measureDto != null) {
      byte[] data = measureDto.getByteData();
      if (data != null) {
        return DsmDataEncoder.decodeDsmData(data);
      }
    }
    return DsmDb.Data.newBuilder().build();
  }

  private static void writeComponents(JsonWriter json, final List<String> componentUuidsWithDependencies, List<ComponentDto> components) {
    json.name("components").beginArray();

    Map<String, ComponentDto> componentsByUuid = componentsByUuid(components);
    for (String uuid : componentUuidsWithDependencies) {
      writeComponent(json, componentsByUuid.get(uuid));
    }

    for (ComponentDto component : componentsWithoutDependenciesSortedByName(componentUuidsWithDependencies, components)) {
      writeComponent(json, component);
    }

    json.endArray();
  }

  private static void writeComponent(JsonWriter json, ComponentDto component){
      json.beginObject()
        .prop("uuid", component.uuid())
        .prop("name", component.name())
        .prop("qualifier", component.qualifier())
        .endObject();
  }

  private static void writeDependencies(JsonWriter json, List<DsmDb.Data.Cell> dsmCells) {
    json.name("dependencies").beginArray();
    for (DsmDb.Data.Cell dsmCell : dsmCells) {
      json.beginObject()
        .prop(Integer.toString(dsmCell.getOffset()), dsmCell.getWeight())
        .endObject();
    }
    json.endArray();
  }

  private static Map<String, ComponentDto> componentsByUuid(List<ComponentDto> components) {
    return Maps.uniqueIndex(components, new NonNullInputFunction<ComponentDto, String>() {
      @Override
      public String doApply(ComponentDto input) {
        return input.uuid();
      }
    });
  }

  private static List<ComponentDto> componentsWithoutDependenciesSortedByName(final List<String> componentUuidsWithDependencies, List<ComponentDto> components){
    List<ComponentDto> componentsWithoutDependencies = newArrayList(components);
    Iterables.removeIf(componentsWithoutDependencies, new NonNullInputPredicate<ComponentDto>() {
      @Override
      public boolean doApply(ComponentDto input) {
        return componentUuidsWithDependencies.contains(input.uuid());
      }
    });

    Collections.sort(componentsWithoutDependencies, COMPONENT_BY_NAME_COMPARATOR);
    return componentsWithoutDependencies;
  }
}
