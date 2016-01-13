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

import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.Result;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class L10nWsTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  DefaultI18n i18n;

  @Mock
  Server server;

  @Test
  public void should_allow_client_to_cache_messages() throws Exception {
    Locale locale = Locale.PRC;
    userSessionRule.setLocale(locale);

    Date now = new Date();
    Date aBitLater = new Date(now.getTime() + 1000);
    when(server.getStartedAt()).thenReturn(now);

    Result result = new WsTester(new L10nWs(i18n, server, userSessionRule)).newGetRequest("api/l10n", "index").setParam("ts", DateUtils.formatDateTime(aBitLater)).execute();
    verifyZeroInteractions(i18n);
    verify(server).getStartedAt();

    result.assertNotModified();
  }

  @Test
  public void should_return_all_l10n_messages_using_accept_header_with_cache_expired() throws Exception {
    Locale locale = Locale.PRC;
    userSessionRule.setLocale(locale);

    Date now = new Date();
    Date aBitEarlier = new Date(now.getTime() - 1000);
    when(server.getStartedAt()).thenReturn(now);

    String key1 = "key1";
    String key2 = "key2";
    String key3 = "key3";

    when(i18n.getPropertyKeys()).thenReturn(ImmutableSet.of(key1, key2, key3));
    when(i18n.message(locale, key1, key1)).thenReturn(key1);
    when(i18n.message(locale, key2, key2)).thenReturn(key2);
    when(i18n.message(locale, key3, key3)).thenReturn(key3);

    Result result = new WsTester(new L10nWs(i18n, server, userSessionRule)).newGetRequest("api/l10n", "index").setParam("ts", DateUtils.formatDateTime(aBitEarlier)).execute();
    verify(i18n).getPropertyKeys();
    verify(i18n).message(locale, key1, key1);
    verify(i18n).message(locale, key2, key2);
    verify(i18n).message(locale, key3, key3);

    result.assertJson("{\"key1\":\"key1\",\"key2\":\"key2\",\"key3\":\"key3\"}");
  }

  @Test
  public void should_override_locale_when_locale_param_is_set() throws Exception {
    Locale locale = Locale.PRC;
    userSessionRule.setLocale(locale);
    Locale override = Locale.JAPANESE;

    String key1 = "key1";
    String key2 = "key2";
    String key3 = "key3";

    when(i18n.getPropertyKeys()).thenReturn(ImmutableSet.of(key1, key2, key3));
    when(i18n.message(override, key1, key1)).thenReturn(key1);
    when(i18n.message(override, key2, key2)).thenReturn(key2);
    when(i18n.message(override, key3, key3)).thenReturn(key3);

    Result result = new WsTester(new L10nWs(i18n, server, userSessionRule)).newGetRequest("api/l10n", "index").setParam("locale", override.toString()).execute();
    verify(i18n).getPropertyKeys();
    verify(i18n).message(override, key1, key1);
    verify(i18n).message(override, key2, key2);
    verify(i18n).message(override, key3, key3);

    result.assertJson("{\"key1\":\"key1\",\"key2\":\"key2\",\"key3\":\"key3\"}");
  }
}
