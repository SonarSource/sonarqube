/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.web;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.user.ServerUserSession;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarLintConnectionFilterIT {
  private static final String LOGIN = "user1";
  private final TestSystem2 system2 = new TestSystem2();

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Test
  public void update() throws IOException {
    system2.setNow(10_000_000L);
    addUser(LOGIN, 1_000_000L);

    runFilter(LOGIN, "SonarLint for IntelliJ");
    assertThat(getLastUpdate(LOGIN)).isEqualTo(10_000_000L);
  }

  @Test
  public void update_first_time() throws IOException {
    system2.setNow(10_000_000L);
    addUser(LOGIN, null);

    runFilter(LOGIN, "SonarLint for IntelliJ");
    assertThat(getLastUpdate(LOGIN)).isEqualTo(10_000_000L);
  }

  @Test
  public void only_applies_to_api() {
    SonarLintConnectionFilter underTest = new SonarLintConnectionFilter(dbTester.getDbClient(), mock(ThreadLocalUserSession.class), system2);
    assertThat(underTest.doGetPattern().matches("/api/test")).isTrue();
    assertThat(underTest.doGetPattern().matches("/test")).isFalse();

  }

  @Test
  public void do_nothing_if_no_sonarlint_agent() throws IOException {
    system2.setNow(10_000L);
    addUser(LOGIN, 1_000L);

    runFilter(LOGIN, "unknown");
    runFilter(LOGIN, null);
    assertThat(getLastUpdate(LOGIN)).isEqualTo(1_000L);
  }

  @Test
  public void do_nothing_if_not_logged_in() throws IOException {
    system2.setNow(10_000_000L);
    addUser("invalid", 1_000_000L);

    runFilter(LOGIN, "SonarLint for IntelliJ");
    assertThat(getLastUpdate("invalid")).isEqualTo(1_000_000L);
  }

  @Test
  public void dont_fail_if_no_user_set() throws IOException {
    SonarLintConnectionFilter underTest = new SonarLintConnectionFilter(dbTester.getDbClient(), new ThreadLocalUserSession(), system2);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getHeader("User-Agent")).thenReturn("sonarlint");
    FilterChain chain = mock(FilterChain.class);
    underTest.doFilter(new JakartaHttpRequest(httpRequest), mock(HttpResponse.class), chain);
    verify(chain).doFilter(any(), any());
  }

  @Test
  public void only_update_if_not_updated_within_1h() throws IOException {
    system2.setNow(2_000_000L);
    addUser(LOGIN, 1_000_000L);

    runFilter(LOGIN, "SonarLint for IntelliJ");
    assertThat(getLastUpdate(LOGIN)).isEqualTo(1_000_000L);
  }

  private void addUser(String login, @Nullable Long lastUpdate) {
    dbTester.users().insertUser(u -> u.setLogin(login).setLastSonarlintConnectionDate(lastUpdate));
  }

  @CheckForNull
  private Long getLastUpdate(String login) {
    return dbTester.getDbClient().userDao().selectByLogin(dbTester.getSession(), login).getLastSonarlintConnectionDate();
  }

  private void runFilter(String loggedInUser, @Nullable String agent) throws IOException {
    UserDto user = dbTester.getDbClient().userDao().selectByLogin(dbTester.getSession(), loggedInUser);
    ThreadLocalUserSession session = new ThreadLocalUserSession();
    session.set(new ServerUserSession(dbTester.getDbClient(), user, false));
    SonarLintConnectionFilter underTest = new SonarLintConnectionFilter(dbTester.getDbClient(), session, system2);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getHeader("User-Agent")).thenReturn(agent);
    FilterChain chain = mock(FilterChain.class);
    underTest.doFilter(new JakartaHttpRequest(httpRequest), mock(HttpResponse.class), chain);
    verify(chain).doFilter(any(), any());
  }
}
