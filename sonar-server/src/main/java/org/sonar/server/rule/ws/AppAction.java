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
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleRepositories.Repository;
import org.sonar.server.user.UserSession;

import java.util.Locale;
import java.util.Map;

/**
 * @since 4.4
 */
public class AppAction implements RequestHandler {

  private final Languages languages;
  private final RuleRepositories ruleRepositories;
  private final I18n i18n;
  private final DebtModel debtModel;
  private final QProfileService qualityProfileService;

  public AppAction(Languages languages, RuleRepositories ruleRepositories, I18n i18n,
                   DebtModel debtModel, QProfileService qualityProfileService) {
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
    json.endObject().close();
  }

  private void addPermissions(JsonWriter json) {
    json.prop("canWrite", UserSession.get().hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN));
  }

  private void addProfiles(JsonWriter json) {
    json.name("qualityprofiles").beginArray();
    for (QualityProfileDto profile : qualityProfileService.findAll()) {
      if (languageIsSupported(profile)) {
        json.beginObject()
          .prop("key", profile.getKey().toString())
          .prop("name", profile.getName())
          .prop("lang", profile.getLanguage())
          .prop("parent", profile.getParent());
        if (profile.getParentKey() != null) {
          json.prop("parentKey", profile.getParentKey().toString());
        }
        json.endObject();
      }
    }
    json.endArray();
  }

  private boolean languageIsSupported(QualityProfileDto profile) {
    return languages.get(profile.getLanguage()) != null;
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

  void define(WebService.NewController controller) {
    controller.createAction("app")
      .setDescription("Data required for rendering the page 'Coding Rules'")
      .setInternal(true)
      .setHandler(this);
  }
}
