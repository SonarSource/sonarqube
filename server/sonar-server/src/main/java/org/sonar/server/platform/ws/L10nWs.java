/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Locale;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;

public class L10nWs implements WebService {

  private final DefaultI18n i18n;
  private final Server server;
  private final UserSession userSession;

  public L10nWs(DefaultI18n i18n, Server server, UserSession userSession) {
    this.i18n = i18n;
    this.server = server;
    this.userSession = userSession;
  }

  @Override
  public void define(Context context) {
    NewController l10n = context.createController("api/l10n");
    l10n.setDescription("Manage localization.")
      .setSince("4.4");
    NewAction indexAction = l10n.createAction("index")
      .setInternal(true)
      .setDescription("Get all localization messages for a given locale")
      .setResponseExample(getClass().getResource("l10n-index-example.json"))
      .setSince("4.4")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          serializeMessages(request, response);
        }
      });
    indexAction.createParam("locale")
      .setDescription("BCP47 language tag, used to override the browser Accept-Language header")
      .setExampleValue("fr-CH");
    indexAction.createParam("ts")
      .setDescription("Date of the last cache update.")
      .setExampleValue("2014-06-04T09:31:42+0000");

    l10n.done();
  }

  private void serializeMessages(Request request, Response response) throws IOException {
    Date timestamp = request.paramAsDateTime("ts");
    if (timestamp != null && timestamp.after(server.getStartedAt())) {
      response.stream().setStatus(HttpURLConnection.HTTP_NOT_MODIFIED).output().close();
    } else {
      Locale locale = userSession.locale();
      String localeParam = request.param("locale");
      if (localeParam != null) {
        locale = Locale.forLanguageTag(localeParam);
        checkArgument(!locale.getISO3Language().isEmpty(), "'%s' cannot be parsed as a BCP47 language tag", localeParam);
      }
      JsonWriter json = response.newJsonWriter().beginObject();
      for (String messageKey : i18n.getPropertyKeys()) {
        json.prop(messageKey, i18n.message(locale, messageKey, messageKey));
      }
      json.endObject().close();
    }
  }
}
