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
package org.sonar.server.rule.ws;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.RuleUpdate;
import org.sonar.server.search.BaseDoc;

public class UpdateAction implements RequestHandler {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_TAGS = "tags";
  public static final String PARAM_MARKDOWN_NOTE = "markdown_note";
  public static final String PARAM_DEBT_SUB_CHARACTERISTIC = "debt_sub_characteristic";
  public static final String PARAM_DEBT_REMEDIATION_FN_TYPE = "debt_remediation_fn_type";
  public static final String PARAM_DEBT_REMEDIATION_FN_OFFSET = "debt_remediation_fn_offset";
  public static final String PARAM_DEBT_REMEDIATION_FY_COEFF = "debt_remediation_fy_coeff";

  private final RuleService service;
  private final RuleMapping mapping;

  public UpdateAction(RuleService service, RuleMapping mapping) {
    this.service = service;
    this.mapping = mapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("update")
      .setPost(true)
      .setDescription("Update an existing rule")
      .setSince("4.4")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Key of the rule to update")
      .setExampleValue("javascript:NullCheck");

    action.createParam(PARAM_TAGS)
      .setDescription("Optional comma-separated list of tags to set. Use blank value to remove current tags. Tags " +
        "are not changed if the parameter is not set.")
      .setExampleValue("java8,security");

    action.createParam(PARAM_MARKDOWN_NOTE)
      .setDescription("Optional note in markdown format. Use empty value to remove current note. Note is not changed" +
        "if the parameter is not set.")
      .setExampleValue("my *note*");

    action.createParam(PARAM_DEBT_SUB_CHARACTERISTIC)
      .setDescription("Optional key of the debt sub-characteristic. Use empty value to unset (-> none) or '" +
        RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC + "' to revert to default sub-characteristic.")
      .setExampleValue("FAULT_TOLERANCE");

    action.createParam(PARAM_DEBT_REMEDIATION_FN_TYPE)
      .setPossibleValues(DebtRemediationFunction.Type.values());

    action.createParam(PARAM_DEBT_REMEDIATION_FN_OFFSET)
      .setExampleValue("1d");

    action.createParam(PARAM_DEBT_REMEDIATION_FY_COEFF)
      .setExampleValue("3min");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RuleUpdate update = readRequest(request);
    service.update(update);
    writeResponse(response, update.getRuleKey());
  }

  private RuleUpdate readRequest(Request request) {
    RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
    RuleUpdate update = new RuleUpdate(key);
    readTags(request, update);
    readMarkdownNote(request, update);
    readDebt(request, update);
    return update;
  }

  private void readTags(Request request, RuleUpdate update) {
    String value = request.param(PARAM_TAGS);
    if (value != null) {
      if (StringUtils.isBlank(value)) {
        update.setTags(null);
      } else {
        update.setTags(Sets.newHashSet(Splitter.on(',').omitEmptyStrings().trimResults().split(value)));
      }
    }
    // else do not touch this field
  }

  private void readMarkdownNote(Request request, RuleUpdate update) {
    String value = request.param(PARAM_MARKDOWN_NOTE);
    if (value != null) {
      update.setMarkdownNote(value);
    }
    // else do not touch this field
  }

  private void readDebt(Request request, RuleUpdate update) {
    String value = request.param(PARAM_DEBT_SUB_CHARACTERISTIC);
    if (value != null) {
      update.setDebtSubCharacteristic(value);
    }
    value = request.param(PARAM_DEBT_REMEDIATION_FN_TYPE);
    if (value != null) {
      if (StringUtils.isBlank(value)) {
        update.setDebtRemediationFunction(null);
      } else {
        DebtRemediationFunction fn = new DefaultDebtRemediationFunction(
          DebtRemediationFunction.Type.valueOf(value), request.param(PARAM_DEBT_REMEDIATION_FY_COEFF),
          request.param(PARAM_DEBT_REMEDIATION_FN_OFFSET));
        update.setDebtRemediationFunction(fn);
      }
    }
  }

  private void writeResponse(Response response, RuleKey ruleKey) {
    Rule rule = service.getByKey(ruleKey);
    JsonWriter json = response.newJsonWriter().beginObject().name("rule");
    mapping.write((BaseDoc) rule, json);
    json.endObject().close();
  }
}
