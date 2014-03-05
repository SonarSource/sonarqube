/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualitygate.db.ProjectQgateAssociation;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationQuery;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.qualitygate.QgateProjectFinder.Association;
import org.sonar.server.qualitygate.QualityGates;

import java.util.Collection;

public class QualityGatesWs implements WebService {

  private static final String PARAM_PAGE_SIZE = "pageSize";
  private static final String PARAM_PAGE = "page";
  private static final String PARAM_QUERY = "query";
  private static final String PARAM_SELECTED = "selected";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_ERROR = "error";
  private static final String PARAM_WARNING = "warning";
  private static final String PARAM_PERIOD = "period";
  private static final String PARAM_OPERATOR = "op";
  private static final String PARAM_METRIC = "metric";
  private static final String PARAM_GATE_ID = "gateId";
  private static final String PARAM_PROJECT_ID = "projectId";
  private static final String PARAM_ID = "id";

  private final QualityGates qualityGates;

  private final QgateProjectFinder projectFinder;

  private final QgateAppHandler appHandler;

  public QualityGatesWs(QualityGates qualityGates, QgateProjectFinder projectFinder, QgateAppHandler appHandler) {
    this.qualityGates = qualityGates;
    this.projectFinder = projectFinder;
    this.appHandler = appHandler;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.newController("api/qualitygates")
      .setSince("4.3")
      .setDescription("This service can be used to manage quality gates, including requirements and project association.");

    defineQualityGateActions(controller);

    defineConditionActions(controller);

    controller.newAction("app")
      .setInternal(true)
      .setDescription("Get initialization items for the admin UI")
      .setSince("4.3")
      .setHandler(appHandler);

    controller.done();
  }

  private void defineConditionActions(NewController controller) {
    NewAction createCondition = controller.newAction("create_condition")
    .setDescription("Add a new condition to a quality gate.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        createCondition(request, response);
      }
    });
    createCondition.newParam(PARAM_GATE_ID).setDescription("The numerical ID of the quality gate for which the condition will be created.");
    addConditionParams(createCondition);

    NewAction updateCondition = controller.newAction("update_condition")
    .setDescription("Update a condition attached to a quality gate.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        updateCondition(request, response);
      }
    });
    updateCondition.newParam(PARAM_ID).setDescription("The numerical ID of the condition to update.");
    addConditionParams(updateCondition);

    controller.newAction("delete_condition")
    .setDescription("Remove a condition from a quality gate.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        deleteCondition(request, response);
      }
    }).newParam(PARAM_ID).setDescription("The numerical ID of the condition to delete.");
  }

  private void addConditionParams(NewAction createCondition) {
    createCondition.newParam(PARAM_METRIC).setDescription("The key for the metric tested by this condition.");
    createCondition.newParam(PARAM_OPERATOR).setDescription("The operator used for the test, one of 'EQ', 'NE', 'LT', 'GT'.");
    createCondition.newParam(PARAM_PERIOD).setDescription("The optional period to use (for differential measures).");
    createCondition.newParam(PARAM_WARNING).setDescription("An optional value for the warning threshold.");
    createCondition.newParam(PARAM_ERROR).setDescription("An optional value for the error threshold.");
  }

  private void defineQualityGateActions(NewController controller) {
    controller.newAction("create")
      .setDescription("Create a quality gate, given its name.")
      .setPost(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          create(request, response);
        }
    }).newParam(PARAM_NAME).setDescription("The name of the quality gate to create.");

    NewAction copy = controller.newAction("copy")
      .setDescription("Copy a quality gate, given its ID and the name for the new quality gate.")
      .setPost(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          copy(request, response);
        }
    });
    copy.newParam(PARAM_ID).setDescription("The ID of the source quality gate.");
    copy.newParam(PARAM_NAME).setDescription("The name of the destination quality gate.");

    controller.newAction("set_as_default")
    .setDescription("Select the default quality gate.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        setDefault(request, response);
      }
    }).newParam(PARAM_ID).setDescription("The ID of the quality gate to use as default.");

    controller.newAction("unset_default")
    .setDescription("Unselect the default quality gate.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        unsetDefault(response);
      }
    });

    NewAction rename = controller.newAction("rename")
    .setDescription("Rename a quality gate, given its id and new name.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        rename(request, response);
      }
    });
    rename.newParam(PARAM_ID).setDescription("The ID of the quality gate to rename.");
    rename.newParam(PARAM_NAME).setDescription("The new name for the quality gate.");

    controller.newAction("list")
    .setDescription("List all quality gates.")
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        list(request, response);
      }
    });

    controller.newAction("show")
    .setDescription("Show a quality gate in details, with associated conditions.")
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        show(request, response);
      }
    }).newParam(PARAM_ID).setDescription("The ID of the quality gate.");

    controller.newAction("destroy")
    .setDescription("Destroy a quality gate, given its id.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        destroy(request, response);
      }
    }).newParam(PARAM_ID).setDescription("The numerical ID of the quality gate to destroy.");

    NewAction search = controller.newAction("search")
    .setDescription("Search projects associated (or not) with a quality gate.")
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        search(request, response);
      }
    });
    search.newParam(PARAM_GATE_ID).setDescription("The numerical ID of the quality gate considered for association.");
    search.newParam(PARAM_SELECTED).setDescription("Optionally, to search for projects associated (selected=selected) or not (selected=deselected).");
    search.newParam(PARAM_QUERY).setDescription("Optionally, part of the name of the projects to search for.");
    search.newParam(PARAM_PAGE);
    search.newParam(PARAM_PAGE_SIZE);

    NewAction select = controller.newAction("select")
      .setPost(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          select(request, response);
        }
      });
    select.newParam(PARAM_GATE_ID);
    select.newParam(PARAM_PROJECT_ID);

    NewAction deselect = controller.newAction("deselect")
      .setPost(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          deselect(request, response);
        }
      });
    deselect.newParam(PARAM_GATE_ID);
    deselect.newParam(PARAM_PROJECT_ID);
  }

  protected void copy(Request request, Response response) {
    QualityGateDto newQualityGate = qualityGates.copy(parseId(request, PARAM_ID), request.mandatoryParam(PARAM_NAME));
    JsonWriter writer = response.newJsonWriter();
    writeQualityGate(newQualityGate, writer).close();
  }

  protected void select(Request request, Response response) {
    qualityGates.associateProject(parseId(request, PARAM_GATE_ID), parseId(request, PARAM_PROJECT_ID));
    response.noContent();
  }

  protected void deselect(Request request, Response response) {
    qualityGates.dissociateProject(parseId(request, PARAM_GATE_ID), parseId(request, PARAM_PROJECT_ID));
    response.noContent();
  }

  protected void search(Request request, Response response) {
    Association associations = projectFinder.find(ProjectQgateAssociationQuery.builder()
      .gateId(request.mandatoryParam(PARAM_GATE_ID))
      .membership(request.param(PARAM_SELECTED))
      .projectSearch(request.param(PARAM_QUERY))
      .pageIndex(request.paramAsInt(PARAM_PAGE))
      .pageSize(request.paramAsInt(PARAM_PAGE_SIZE))
      .build());
    JsonWriter writer = response.newJsonWriter();
    writer.beginObject().prop("more", associations.hasMoreResults());
    writer.name("results").beginArray();
    for (ProjectQgateAssociation project: associations.projects()) {
      writer.beginObject().prop("id", project.id()).prop("name", project.name()).prop(PARAM_SELECTED, project.isMember()).endObject();
    }
    writer.endArray().endObject().close();
  }

  protected void show(Request request, Response response) {
    final Long qGateId = parseId(request, PARAM_ID);
    QualityGateDto qGate = qualityGates.get(qGateId);
    JsonWriter writer = response.newJsonWriter().beginObject()
      .prop(PARAM_ID, qGate.getId())
      .prop(PARAM_NAME, qGate.getName());
    Collection<QualityGateConditionDto> conditions = qualityGates.listConditions(qGateId);
    if (!conditions.isEmpty()) {
      writer.name("conditions").beginArray();
      for (QualityGateConditionDto condition: conditions) {
        writeQualityGateCondition(condition, writer);
      }
      writer.endArray();
    }
    writer.endObject().close();
  }

  protected void createCondition(Request request, Response response) {
    writeQualityGateCondition(
      qualityGates.createCondition(
        parseId(request, PARAM_GATE_ID),
        request.mandatoryParam(PARAM_METRIC),
        request.mandatoryParam(PARAM_OPERATOR),
        request.param(PARAM_WARNING),
        request.param(PARAM_ERROR),
        request.paramAsInt(PARAM_PERIOD)
      ), response.newJsonWriter()).close();
  }

  protected void updateCondition(Request request, Response response) {
    writeQualityGateCondition(
      qualityGates.updateCondition(
        parseId(request, PARAM_ID),
        request.mandatoryParam(PARAM_METRIC),
        request.mandatoryParam(PARAM_OPERATOR),
        request.param(PARAM_WARNING),
        request.param(PARAM_ERROR),
        request.paramAsInt(PARAM_PERIOD)
      ), response.newJsonWriter()).close();
  }

  protected void deleteCondition(Request request, Response response) {
    qualityGates.deleteCondition(parseId(request, PARAM_ID));
    response.noContent();
  }

  protected void setDefault(Request request, Response response) {
    qualityGates.setDefault(parseId(request, PARAM_ID));
    response.noContent();
  }

  protected void unsetDefault(Response response) {
    qualityGates.setDefault(null);
    response.noContent();
  }

  protected void rename(Request request, Response response) {
    long idToRename = parseId(request, PARAM_ID);
    QualityGateDto renamedQualityGate = qualityGates.rename(idToRename, request.mandatoryParam(PARAM_NAME));
    JsonWriter writer = response.newJsonWriter();
    writeQualityGate(renamedQualityGate, writer).close();
  }

  protected void destroy(Request request, Response response) {
    qualityGates.delete(parseId(request, PARAM_ID));
    response.noContent();
  }

  protected void list(Request request, Response response) {
    JsonWriter writer = response.newJsonWriter().beginObject().name("qualitygates").beginArray();
    for (QualityGateDto qgate: qualityGates.list()) {
      writeQualityGate(qgate, writer);
    }
    writer.endArray();
    QualityGateDto defaultQgate = qualityGates.getDefault();
    if (defaultQgate != null) {
      writer.prop("default", defaultQgate.getId());
    }
    writer.endObject().close();
  }

  protected void create(Request request, Response response) {
    QualityGateDto newQualityGate = qualityGates.create(request.mandatoryParam(PARAM_NAME));
    JsonWriter writer = response.newJsonWriter();
    writeQualityGate(newQualityGate, writer).close();
  }

  private Long parseId(Request request, String paramName) {
    try {
      return Long.valueOf(request.mandatoryParam(paramName));
    } catch (NumberFormatException badFormat) {
      throw new BadRequestException(paramName + " must be a valid long value");
    }
  }

  private JsonWriter writeQualityGate(QualityGateDto qualityGate, JsonWriter writer) {
    return writer.beginObject()
      .prop(PARAM_ID, qualityGate.getId())
      .prop(PARAM_NAME, qualityGate.getName())
      .endObject();
  }

  private JsonWriter writeQualityGateCondition(QualityGateConditionDto condition, JsonWriter writer) {
    writer.beginObject()
      .prop(PARAM_ID, condition.getId())
      .prop(PARAM_METRIC, condition.getMetricKey())
      .prop(PARAM_OPERATOR, condition.getOperator());
    if(condition.getWarningThreshold() != null) {
      writer.prop(PARAM_WARNING, condition.getWarningThreshold());
    }
    if(condition.getErrorThreshold() != null) {
      writer.prop(PARAM_ERROR, condition.getErrorThreshold());
    }
    if(condition.getPeriod() != null) {
      writer.prop(PARAM_PERIOD, condition.getPeriod());
    }
    writer.endObject();
    return writer;
  }

}
