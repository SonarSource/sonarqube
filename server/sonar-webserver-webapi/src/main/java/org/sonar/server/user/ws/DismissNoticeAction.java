/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user.ws;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.EDUCATION_PRINCIPLES;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.ISSUE_CLEAN_CODE_GUIDE;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.ISSUE_NEW_ISSUE_STATUS_AND_TRANSITION_GUIDE;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.SHOW_DNA_BANNER;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.SHOW_DNA_OPTIN_BANNER;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.SHOW_DNA_TOUR;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.SHOW_ENABLE_SCA;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.SHOW_NEW_MODES_BANNER;
import static org.sonar.server.user.ws.DismissNoticeAction.DismissNotices.SHOW_NEW_MODES_TOUR;

public class DismissNoticeAction implements UsersWsAction {

  public enum DismissNotices {
    EDUCATION_PRINCIPLES("educationPrinciples"),
    SONARLINT_AD("sonarlintAd"),
    ISSUE_CLEAN_CODE_GUIDE("issueCleanCodeGuide"),
    OVERVIEW_ZERO_NEW_ISSUES_SIMPLIFICATION("overviewZeroNewIssuesSimplification"),
    ISSUE_NEW_ISSUE_STATUS_AND_TRANSITION_GUIDE("issueNewIssueStatusAndTransitionGuide"),
    SHOW_NEW_MODES_TOUR("showNewModesTour"),
    SHOW_NEW_MODES_BANNER("showNewModesBanner"),
    SHOW_DNA_OPTIN_BANNER("showDesignAndArchitectureOptInBanner"),
    SHOW_DNA_BANNER("showDesignAndArchitectureBanner"),
    SHOW_DNA_TOUR("showDesignAndArchitectureTour"),
    SHOW_ENABLE_SCA("showEnableSca"),
    ;

    private final String key;

    DismissNotices(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    public static Set<String> getAvailableKeys() {
      return Arrays.stream(values())
        .map(DismissNotices::getKey)
        .collect(Collectors.toSet());
    }
  }
  public static final String USER_DISMISS_CONSTANT = "user.dismissedNotices.";
  private static final String SUPPORT_FOR_NEW_NOTICE_MESSAGE = "Support for new notice '%s' was added.";

  private final UserSession userSession;
  private final DbClient dbClient;

  public DismissNoticeAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  private static String printNewNotice(DismissNotices notice) {
    return SUPPORT_FOR_NEW_NOTICE_MESSAGE.formatted(notice.getKey());
  }

  private static String printNewNotice(DismissNotices... notices) {
    String noticesList = Arrays.stream(notices)
      .map(DismissNotices::getKey)
      .collect(Collectors.joining(", "));
    return SUPPORT_FOR_NEW_NOTICE_MESSAGE.formatted(noticesList);
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("dismiss_notice")
      .setDescription("Dismiss a notice for the current user. Silently ignore if the notice is already dismissed.")
      .setChangelog(new Change("2025.3", printNewNotice(SHOW_ENABLE_SCA)))
      .setChangelog(new Change("2025.3", printNewNotice(SHOW_DNA_OPTIN_BANNER, SHOW_DNA_BANNER, SHOW_DNA_TOUR)))
      .setChangelog(new Change("10.8", printNewNotice(SHOW_NEW_MODES_TOUR)))
      .setChangelog(new Change("10.8", printNewNotice(SHOW_NEW_MODES_BANNER)))
      .setChangelog(new Change("10.4", printNewNotice(ISSUE_NEW_ISSUE_STATUS_AND_TRANSITION_GUIDE)))
      .setChangelog(new Change("10.2", printNewNotice(ISSUE_CLEAN_CODE_GUIDE)))
      .setSince("9.6")
      .setInternal(true)
      .setHandler(this)
      .setPost(true);

    action.createParam("notice")
      .setDescription("notice key to dismiss")
      .setExampleValue(EDUCATION_PRINCIPLES)
      .setPossibleValues(DismissNotices.getAvailableKeys());
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String currentUserUuid = userSession.getUuid();
    checkState(currentUserUuid != null, "User uuid should not be null");

    String noticeKeyParam = request.mandatoryParam("notice");

    dismissNotice(response, currentUserUuid, noticeKeyParam);
  }

  private void dismissNotice(Response response, String currentUserUuid, String noticeKeyParam) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String paramKey = USER_DISMISS_CONSTANT + noticeKeyParam;
      PropertyQuery query = new PropertyQuery.Builder()
        .setUserUuid(currentUserUuid)
        .setKey(paramKey)
        .build();

      if (dbClient.propertiesDao().selectByQuery(query, dbSession).isEmpty()) {
        PropertyDto property = new PropertyDto().setUserUuid(currentUserUuid).setKey(paramKey);
        dbClient.propertiesDao().saveProperty(dbSession, property);
        dbSession.commit();
      }

      response.noContent();
    }
  }
}
