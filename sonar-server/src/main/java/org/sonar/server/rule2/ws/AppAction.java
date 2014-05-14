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
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfiles;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleRepositories.Repository;

import java.util.Locale;
import java.util.Map;

/**
 * @since 4.4
 */
public class AppAction implements RequestHandler {

  private static final String[] MESSAGES = {
    "all", // All
    "any", // Any
    "apply", // Apply
    "are_you_sure", // Are you sure?
    "bold", // Bold
    "bulk_change", // Bulk Change
    "bulleted_point", // Bulleted point
    "cancel", // Cancel
    "change_verb", // Change
    "code", // Code
    "delete", // Delete
    "Done", // Done
    "edit", // Edit
    "markdown.helplink", // Markdown Help
    "moreCriteria", // + More Criteria
    "save", // Save
    "search_verb", // Search
    "severity", // Severity
    "update_verb", // Update

    "severity.BLOCKER", // Blocker
    "severity.CRITICAL", // Critical
    "severity.MAJOR", // Major
    "severity.MINOR", // Minor
    "severity.INFO", // Info

    "coding_rules.activate", // Activate
    "coding_rules.activate_in", // Activate In
    "coding_rules.activate_in_quality_profile", // Activate In Quality Profile
    "coding_rules.activate_in_all_quality_profiles", // Activate In All {0} Profiles
    "coding_rules.add_note", // Add Note
    "coding_rules.available_since", // Available Since
    "coding_rules.bulk_change", // Bulk Change
    "coding_rules.change_severity", // Change Severity
    "coding_rules.change_severity_in", // Change Severity In
    "coding_rules.change_details", // Change Details of Quality Profile
    "coding_rules.extend_description", // Extend Description
    "coding_rules.deactivate_in", // Deactivate In
    "coding_rules.deactivate", // Deactivate
    "coding_rules.deactivate_in_quality_profile", // Deactivate In Quality Profile
    "coding_rules.deactivate_in_all_quality_profiles", // Deactivate In All {0} Profiles
    "coding_rules.found", // Found
    "coding_rules.inherits", // "{0}" inherits "{1}"
    "coding_rules.key", // Key:
    "coding_rules.new_search", // New Search
    "coding_rules.no_results", // No Coding Rules
    "coding_rules.no_tags", // No tags
    "coding_rules.order", // Order
    "coding_rules.ordered_by", // Ordered By
    "coding_rules.original", // Original:
    "coding_rules.page", // Coding Rules
    "coding_rules.parameters", // Parameters
    "coding_rules.parameters.default_value", // Default Value:
    "coding_rules.permalink", // Permalink
    "coding_rules.quality_profiles", // Quality Profiles
    "coding_rules.quality_profile", // Quality Profile
    "coding_rules.repository", // Repository:
    "coding_rules.revert_to_parent_definition", // Revert to Parent Definition
    "coding_rules._rules", // rules
    "coding_rules.select_tag", // Select Tag

    "coding_rules.filters.activation", // Activation
    "coding_rules.filters.activation.active", // Active
    "coding_rules.filters.activation.inactive", // Inactive
    "coding_rules.filters.activation.help", // Activation criterion is available when a quality profile is selected
    "coding_rules.filters.availableSince", // Available Since
    "coding_rules.filters.characteristic", // Characteristic
    "coding_rules.filters.description", // Description
    "coding_rules.filters.quality_profile", // Quality Profile
    "coding_rules.filters.inheritance", // Inheritance
    "coding_rules.filters.inheritance.inactive", // Inheritance criterion is available when an inherited quality profile is selected
    "coding_rules.filters.inheritance.not_inherited", // Not Inherited
    "coding_rules.filters.inheritance.inherited", // Inherited
    "coding_rules.filters.inheritance.overriden", // Overriden
    "coding_rules.filters.key", // Key
    "coding_rules.filters.language", // Language
    "coding_rules.filters.name", // Name
    "coding_rules.filters.repository", // Repository
    "coding_rules.filters.severity", // Severity
    "coding_rules.filters.status", // Status
    "coding_rules.filters.tag", // Tag

    "coding_rules.sort.creation_date", // Creation Date
    "coding_rules.sort.name" // Name
  };

  private final Languages languages;
  private final RuleRepositories ruleRepositories;
  private final I18n i18n;
  private final DebtModel debtModel;
  private final QProfiles qProfiles;

  public AppAction(Languages languages, RuleRepositories ruleRepositories, I18n i18n, DebtModel debtModel, QProfiles qProfiles) {
    this.languages = languages;
    this.ruleRepositories = ruleRepositories;
    this.i18n = i18n;
    this.debtModel = debtModel;
    this.qProfiles = qProfiles;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    addProfiles(json);
    addLanguages(json);
    addRuleRepositories(json);
    addStatuses(json);
    addCharacteristics(json);
    addMessages(json);
    json.endObject().close();
  }

  private void addProfiles(JsonWriter json) {
    json.name("qualityprofiles").beginArray();
    for (QProfile profile: qProfiles.allProfiles()) {
      json.beginObject()
        .prop("name", profile.name())
        .prop("lang", profile.language())
        .prop("parent", profile.parent())
        .endObject();
    }
    json.endArray();
  }

  private void addLanguages(JsonWriter json) {
    json.name("languages").beginObject();
    for (Language language: languages.all()) {
      json.prop(language.getKey(), language.getName());
    }
    json.endObject();
  }

  private void addRuleRepositories(JsonWriter json) {
    json.name("repositories").beginArray();
    for (Repository repo: ruleRepositories.repositories()) {
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
    for (RuleStatus status: RuleStatus.values()) {
      if (status != RuleStatus.REMOVED) {
        json.prop(status.toString(), i18n.message(Locale.getDefault(), "rules.status." + status.toString().toLowerCase(), status.toString()));
      }
    }
    json.endObject();
  }

  private void addCharacteristics(JsonWriter json) {
    Map<Integer, DefaultDebtCharacteristic> caracById = Maps.newHashMap();
    for (DebtCharacteristic carac: debtModel.allCharacteristics()) {
      DefaultDebtCharacteristic fullCarac = (DefaultDebtCharacteristic) carac;
      caracById.put(fullCarac.id(), fullCarac);
    }
    json.name("characteristics").beginObject();
    for (DefaultDebtCharacteristic carac: caracById.values()) {
      json.prop(carac.key(), carac.isSub() ? caracById.get(carac.parentId()).name() + ": " + carac.name() : carac.name());
    }
    json.endObject();
  }

  private void addMessages(JsonWriter json) {
    json.name("messages").beginObject();
    for (String message: MESSAGES) {
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
