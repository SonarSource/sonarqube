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
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualitygate.QualityGates;

import java.util.Collection;

public class QualityGatesWs implements WebService {

  private static final String PARAM_ERROR = "error";
  private static final String PARAM_WARNING = "warning";
  private static final String PARAM_PERIOD = "period";
  private static final String PARAM_OPERATOR = "op";
  private static final String PARAM_METRIC = "metric";
  private static final String PARAM_GATE_ID = "gateId";
  private final QualityGates qualityGates;

  public QualityGatesWs(QualityGates qualityGates) {
    this.qualityGates = qualityGates;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.newController("api/qualitygates")
      .setSince("4.3")
      .setDescription("This service can be used to manage quality gates, including requirements and project association.");

    controller.newAction("create")
      .setDescription("Create a quality gate, given its name.")
      .setPost(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          create(request, response);
        }
    }).newParam("name").setDescription("The name of the quality gate to create.");

    controller.newAction("set_as_default")
    .setDescription("Select the default quality gate.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        setDefault(request, response);
      }
    }).newParam("id").setDescription("The ID of the quality gate to use as default.");

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
    rename.newParam("id").setDescription("The ID of the quality gate to rename.");
    rename.newParam("name").setDescription("The new name for the quality gate.");

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
    }).newParam("id").setDescription("The ID of the quality gate.");

    controller.newAction("destroy")
    .setDescription("Destroy a quality gate, given its id.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        destroy(request, response);
      }
    }).newParam("id").setDescription("The numerical ID of the quality gate to destroy.");

    NewAction createCondition = controller.newAction("create_condition")
    .setDescription("Add a new condition to a quality gate.")
    .setPost(true)
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        createCondition(request, response);
      }
    });
    createCondition.newParam(PARAM_GATE_ID).setDescription("The numerical ID of the quality gate.");
    createCondition.newParam(PARAM_METRIC).setDescription("The key for the metric tested by this condition.");
    createCondition.newParam(PARAM_OPERATOR).setDescription("The operator used for the test, one of 'EQ', 'NE', 'LT', 'GT'.");
    createCondition.newParam(PARAM_PERIOD).setDescription("The optional period to use (for differential measures).");
    createCondition.newParam(PARAM_WARNING).setDescription("An optional value for the warning threshold.");
    createCondition.newParam(PARAM_ERROR).setDescription("An optional value for the error threshold.");

    controller.done();
  }

  protected void show(Request request, Response response) {
    final Long qGateId = parseId(request, "id");
    QualityGateDto qGate = qualityGates.get(qGateId);
    JsonWriter writer = response.newJsonWriter().beginObject()
      .prop("id", qGate.getId())
      .prop("name", qGate.getName());
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
        request.requiredParam(PARAM_METRIC),
        request.requiredParam(PARAM_OPERATOR),
        request.param(PARAM_WARNING),
        request.param(PARAM_ERROR),
        request.intParam(PARAM_PERIOD)
      ), response.newJsonWriter()).close();;
  }

  protected void setDefault(Request request, Response response) {
    qualityGates.setDefault(parseId(request, "id"));
    response.noContent();
  }

  protected void unsetDefault(Response response) {
    qualityGates.setDefault(null);
    response.noContent();
  }

  protected void rename(Request request, Response response) {
    long idToRename = parseId(request, "id");
    QualityGateDto renamedQualityGate = qualityGates.rename(idToRename, request.requiredParam("name"));
    JsonWriter writer = response.newJsonWriter();
    writeQualityGate(renamedQualityGate, writer).close();
  }

  protected void destroy(Request request, Response response) {
    qualityGates.delete(parseId(request, "id"));
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
    QualityGateDto newQualityGate = qualityGates.create(request.requiredParam("name"));
    JsonWriter writer = response.newJsonWriter();
    writeQualityGate(newQualityGate, writer).close();
  }

  private Long parseId(Request request, String paramName) {
    try {
      return Long.valueOf(request.requiredParam(paramName));
    } catch (NumberFormatException badFormat) {
      throw new BadRequestException(paramName + " must be a valid long value");
    }
  }

  private JsonWriter writeQualityGate(QualityGateDto newQualityGate, JsonWriter writer) {
    return writer.beginObject()
      .prop("id", newQualityGate.getId())
      .prop("name", newQualityGate.getName())
      .endObject();
  }

  private JsonWriter writeQualityGateCondition(QualityGateConditionDto condition, JsonWriter writer) {
    writer.beginObject()
      .prop("id", condition.getId())
      .prop("metric", condition.getMetricKey())
      .prop("op", condition.getOperator());
    if(condition.getWarningThreshold() != null) {
      writer.prop("warning", condition.getWarningThreshold());
    }
    if(condition.getErrorThreshold() != null) {
      writer.prop("error", condition.getErrorThreshold());
    }
    if(condition.getPeriod() != null) {
      writer.prop("period", condition.getPeriod());
    }
    writer.endObject();
    return writer;
  }

}
