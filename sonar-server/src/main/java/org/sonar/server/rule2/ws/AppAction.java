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
package org.sonar.server.rule2.ws;

import org.elasticsearch.common.collect.Maps;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.qualityprofile.QualityProfileService;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleRepositories.Repository;
import org.sonar.server.user.UserSession;

import java.util.Locale;
import java.util.Map;

/**
 * @since 4.4
 */
public class AppAction implements RequestHandler {

  private static final String[] MESSAGES = {
    "all",
    "any",
    "apply",
    "are_you_sure",
    "bold",
    "bulk_change",
    "bulleted_point",
    "cancel",
    "change_verb",
    "code",
    "delete",
    "Done",
    "edit",
    "markdown.helplink",
    "moreCriteria",
    "save",
    "search_verb",
    "severity",
    "update_verb",

    "severity.BLOCKER",
    "severity.CRITICAL",
    "severity.MAJOR",
    "severity.MINOR",
    "severity.INFO",

    "coding_rules.activate",
    "coding_rules.activate_in",
    "coding_rules.activate_in_quality_profile",
    "coding_rules.activate_in_all_quality_profiles",
    "coding_rules.add_note",
    "coding_rules.add_tags",
    "coding_rules.available_since",
    "coding_rules.bulk_change",
    "coding_rules.change_severity",
    "coding_rules.change_severity_in",
    "coding_rules.change_details",
    "coding_rules.extend_description",
    "coding_rules.deactivate_in",
    "coding_rules.deactivate",
    "coding_rules.deactivate_in_quality_profile",
    "coding_rules.deactivate_in_all_quality_profiles",
    "coding_rules.found",
    "coding_rules.inherits",
    "coding_rules.key",
    "coding_rules.new_search",
    "coding_rules.no_results",
    "coding_rules.no_tags",
    "coding_rules.order",
    "coding_rules.ordered_by",
    "coding_rules.original",
    "coding_rules.page",
    "coding_rules.parameters",
    "coding_rules.parameters.default_value",
    "coding_rules.permalink",
    "coding_rules.quality_profiles",
    "coding_rules.quality_profile",
    "coding_rules.repository",
    "coding_rules.revert_to_parent_definition",
    "coding_rules._rules",
    "coding_rules.select_tag",

    "coding_rules.filters.activation",
    "coding_rules.filters.activation.active",
    "coding_rules.filters.activation.inactive",
    "coding_rules.filters.activation.help",
    "coding_rules.filters.availableSince",
    "coding_rules.filters.characteristic",
    "coding_rules.filters.description",
    "coding_rules.filters.quality_profile",
    "coding_rules.filters.inheritance",
    "coding_rules.filters.inheritance.inactive",
    "coding_rules.filters.inheritance.not_inherited",
    "coding_rules.filters.inheritance.inherited",
    "coding_rules.filters.inheritance.overriden",
    "coding_rules.filters.key",
    "coding_rules.filters.language",
    "coding_rules.filters.name",
    "coding_rules.filters.repository",
    "coding_rules.filters.severity",
    "coding_rules.filters.status",
    "coding_rules.filters.tag",

    "coding_rules.facets.tags",
    "coding_rules.facets.languages",
    "coding_rules.facets.repositories",

    "coding_rules.sort.creation_date",
    "coding_rules.sort.name"
  };

  private final Languages languages;
  private final RuleRepositories ruleRepositories;
  private final I18n i18n;
  private final DebtModel debtModel;
  private final QualityProfileService qualityProfileService;

  public AppAction(Languages languages, RuleRepositories ruleRepositories, I18n i18n, DebtModel debtModel, QualityProfileService qualityProfileService) {
    this.languages = languages;
    this.ruleRepositories = ruleRepositories;
    this.i18n = i18n;
    this.debtModel = debtModel;
    this.qualityProfileService = qualityProfileService;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    addPermissions(json);
    addProfiles(json);
    addLanguages(json);
    addRuleRepositories(json);
    addStatuses(json);
    addCharacteristics(json);
    addMessages(json);
    json.endObject().close();
  }

  private void addPermissions(JsonWriter json) {
    json.prop("canWrite", UserSession.get().hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN));
  }

  private void addProfiles(JsonWriter json) {
    json.name("qualityprofiles").beginArray();
    for (QualityProfileDto profile : qualityProfileService.findAll()) {
      json.beginObject()
        .prop("key", profile.getKey().toString())
        .prop("name", profile.getName())
        .prop("lang", profile.getLanguage())
        .prop("parent", profile.getParent());
      if (profile.getParentKey() != null) {
        json
          .prop("parentKey", profile.getParentKey().toString());

      }
      json
        .endObject();
    }
    json.endArray();
  }

  private void addLanguages(JsonWriter json) {
    json.name("languages").beginObject();
    for (Language language : languages.all()) {
      json.prop(language.getKey(), language.getName());
    }
    json.endObject();
  }

  private void addRuleRepositories(JsonWriter json) {
    json.name("repositories").beginArray();
    for (Repository repo : ruleRepositories.repositories()) {
      json.beginObject()
        .prop("key", repo.key())
        .prop("name", repo.name())
        .prop("language", repo.language())
        .endObject();
    }
    json.endArray();
  }

  private void addStatuses(JsonWriter json) {
    json.name("statuses").beginObject();
    for (RuleStatus status : RuleStatus.values()) {
      if (status != RuleStatus.REMOVED) {
        json.prop(status.toString(), i18n.message(Locale.getDefault(), "rules.status." + status.toString().toLowerCase(), status.toString()));
      }
    }
    json.endObject();
  }

  private void addCharacteristics(JsonWriter json) {
    Map<Integer, DefaultDebtCharacteristic> caracById = Maps.newHashMap();
    for (DebtCharacteristic carac : debtModel.allCharacteristics()) {
      DefaultDebtCharacteristic fullCarac = (DefaultDebtCharacteristic) carac;
      caracById.put(fullCarac.id(), fullCarac);
    }
    json.name("characteristics").beginObject();
    for (DefaultDebtCharacteristic carac : caracById.values()) {
      json.prop(carac.key(), carac.isSub() ? caracById.get(carac.parentId()).name() + ": " + carac.name() : carac.name());
    }
    json.endObject();
  }

  private void addMessages(JsonWriter json) {
    json.name("messages").beginObject();
    for (String message : MESSAGES) {
      json.prop(message, i18n.message(Locale.getDefault(), message, message));
    }
    json.endObject();
  }

  void define(WebService.NewController controller) {
    controller.createAction("app")
      .setDescription("Data required for rendering the page 'Coding Rules'")
      .setInternal(true)
      .setHandler(this);
  }
}
