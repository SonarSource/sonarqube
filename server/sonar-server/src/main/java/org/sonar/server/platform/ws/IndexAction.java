/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.ws;

import java.util.Date;
import java.util.Locale;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.server.ws.WsAction;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.util.Locale.ENGLISH;

public class IndexAction implements WsAction {

  private static final String LOCALE_PARAM = "locale";
  private static final String TS_PARAM = "ts";
  private final DefaultI18n i18n;
  private final Server server;

  public IndexAction(DefaultI18n i18n, Server server) {
    this.i18n = i18n;
    this.server = server;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction indexAction = context.createAction("index")
      .setInternal(true)
      .setDescription("Get all localization messages for a given locale")
      .setResponseExample(getClass().getResource("l10n-index-example.json"))
      .setSince("4.4")
      .setHandler(this);
    indexAction.createParam(LOCALE_PARAM)
      .setDescription("BCP47 language tag, used to override the browser Accept-Language header")
      .setExampleValue("fr-CH")
      .setDefaultValue(ENGLISH.toLanguageTag());
    indexAction.createParam(TS_PARAM)
      .setDescription("Date of the last cache update.")
      .setExampleValue("2014-06-04T09:31:42+0000");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Date timestamp = request.paramAsDateTime(TS_PARAM);
    if (timestamp != null && timestamp.after(server.getStartedAt())) {
      response.stream().setStatus(HTTP_NOT_MODIFIED).output().close();
      return;
    }
    String localeParam = request.mandatoryParam(LOCALE_PARAM);
    Locale locale = Locale.forLanguageTag(localeParam);
    checkArgument(!locale.getISO3Language().isEmpty(), "'%s' cannot be parsed as a BCP47 language tag", localeParam);

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      json.prop("effectiveLocale", i18n.getEffectiveLocale(locale).toLanguageTag());
      json.name("messages");
      json.beginObject();
      i18n.getPropertyKeys().forEach(messageKey -> json.prop(messageKey, i18n.message(locale, messageKey, messageKey)));
      json.endObject();
      json.endObject();
    }
  }
}
