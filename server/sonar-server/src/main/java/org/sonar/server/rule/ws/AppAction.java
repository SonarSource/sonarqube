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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleRepositories.Repository;
import org.sonar.server.user.UserSession;

/**
 * @since 4.4
 */
public class AppAction implements RulesWsAction {

  private final Languages languages;
  private final RuleRepositories ruleRepositories;
  private final I18n i18n;
  private final DebtModel debtModel;
  private final QProfileLoader profileLoader;
  private final UserSession userSession;

  public AppAction(Languages languages, RuleRepositories ruleRepositories, I18n i18n,
    DebtModel debtModel, QProfileLoader profileLoader, UserSession userSession) {
    this.languages = languages;
    this.ruleRepositories = ruleRepositories;
    this.i18n = i18n;
    this.debtModel = debtModel;
    this.profileLoader = profileLoader;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("app")
      .setDescription("Data required for rendering the page 'Coding Rules'")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.5")
      .setInternal(true)
      .setHandler(this);
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
    json.prop("canWrite", userSession.hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN));
  }

  private void addProfiles(JsonWriter json) {
    json.name("qualityprofiles").beginArray();
    for (QualityProfileDto profile : profileLoader.findAll()) {
      if (languageIsSupported(profile)) {
        json
          .beginObject()
          .prop("key", profile.getKey())
          .prop("name", profile.getName())
          .prop("lang", profile.getLanguage())
          .prop("parentKey", profile.getParentKee())
          .endObject();
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

    List<DebtCharacteristic> rootCharacs = sortRootCaracs();

    Multimap<Integer, DefaultDebtCharacteristic> subByCaracId = ventilateSubCaracs(rootCharacs);

    json.name("characteristics").beginArray();
    for (DebtCharacteristic rootCarac : rootCharacs) {
      json.beginObject()
        .prop("key", rootCarac.key())
        .prop("name", rootCarac.name())
        .endObject();
      for (DefaultDebtCharacteristic child : sortSubCaracs(subByCaracId, rootCarac)) {
        json.beginObject()
          .prop("key", child.key())
          .prop("name", child.name())
          .prop("parent", rootCarac.key())
          .endObject();
      }
    }
    json.endArray();
  }

  private List<DebtCharacteristic> sortRootCaracs() {
    List<DebtCharacteristic> rootCharacs = Lists.newArrayList(debtModel.characteristics());
    Collections.sort(rootCharacs, new Comparator<DebtCharacteristic>() {
      @Override
      public int compare(DebtCharacteristic o1, DebtCharacteristic o2) {
        return new CompareToBuilder().append(o1.order(), o2.order()).toComparison();
      }
    });
    return rootCharacs;

  }

  private Multimap<Integer, DefaultDebtCharacteristic> ventilateSubCaracs(List<DebtCharacteristic> rootCharacs) {
    Multimap<Integer, DefaultDebtCharacteristic> subByCaracId = LinkedListMultimap.create(rootCharacs.size());

    for (DebtCharacteristic carac : debtModel.allCharacteristics()) {
      DefaultDebtCharacteristic fullCarac = (DefaultDebtCharacteristic) carac;
      if (carac.isSub()) {
        subByCaracId.put(fullCarac.parentId(), fullCarac);
      }
    }
    return subByCaracId;
  }

  private List<DefaultDebtCharacteristic> sortSubCaracs(Multimap<Integer, DefaultDebtCharacteristic> subByCaracId, DebtCharacteristic rootCarac) {
    List<DefaultDebtCharacteristic> subCaracs = Lists.newArrayList(subByCaracId.get(((DefaultDebtCharacteristic) rootCarac).id()));
    Collections.sort(subCaracs, new Comparator<DefaultDebtCharacteristic>() {
      @Override
      public int compare(DefaultDebtCharacteristic o1, DefaultDebtCharacteristic o2) {
        return o1.name().compareTo(o2.name());
      }
    });
    return subCaracs;
  }
}
