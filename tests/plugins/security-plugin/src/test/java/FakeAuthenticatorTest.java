/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.security.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;


public class FakeAuthenticatorTest {

  private Settings settings;
  private FakeAuthenticator authenticator;

  @Before
  public void setUp() {
    settings = new MapSettings();
    authenticator = new FakeAuthenticator(settings);
    authenticator.init();
  }

  @Test
  public void shouldNeverTouchAdmin() {
    assertThat(authenticator.authenticate("admin", "admin")).isTrue();
    assertThat(authenticator.doGetGroups("admin")).isNull();
    assertThat(authenticator.doGetUserDetails("admin")).isNull();
  }

  @Test
  public void shouldAuthenticateFakeUsers() {
    settings.setProperty(FakeAuthenticator.DATA_PROPERTY, "evgeny.password=foo");

    assertThat(authenticator.authenticate("evgeny", "foo")).isTrue();
    assertThat(authenticator.authenticate("evgeny", "bar")).isFalse();
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotAuthenticateNotExistingUsers() {
    authenticator.authenticate("evgeny", "foo");
  }

  @Test
  public void shouldGetUserDetails() {
    settings.setProperty(FakeAuthenticator.DATA_PROPERTY, "evgeny.password=foo\n" +
      "evgeny.name=Tester Testerovich\n" +
      "evgeny.email=evgeny@example.org");

    UserDetails details = authenticator.doGetUserDetails("evgeny");
    assertThat(details.getName()).isEqualTo("Tester Testerovich");
    assertThat(details.getEmail()).isEqualTo("evgeny@example.org");
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotReturnDetailsForNotExistingUsers() {
    authenticator.doGetUserDetails("evgeny");
  }

  @Test
  public void shouldGetGroups() {
    settings.setProperty(FakeAuthenticator.DATA_PROPERTY, "evgeny.password=foo\n" +
      "evgeny.groups=sonar-users,sonar-developers");

    assertThat(authenticator.doGetGroups("evgeny")).containsOnly("sonar-users", "sonar-developers");
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotReturnGroupsForNotExistingUsers() {
    authenticator.doGetGroups("evgeny");
  }

  @Test
  public void shouldParseList() {
    assertThat(FakeAuthenticator.parseList(null)).isEmpty();
    assertThat(FakeAuthenticator.parseList("")).isEmpty();
    assertThat(FakeAuthenticator.parseList(",,,")).isEmpty();
    assertThat(FakeAuthenticator.parseList("a,b")).containsOnly("a", "b");
  }

  @Test
  public void shouldParseMap() {
    Map<String, String> map = FakeAuthenticator.parse(null);
    assertThat(map).isEmpty();

    map = FakeAuthenticator.parse("");
    assertThat(map).isEmpty();

    map = FakeAuthenticator.parse("foo=bar");
    assertThat(map).hasSize(1);
    assertThat(map.get("foo")).isEqualTo("bar");

    map = FakeAuthenticator.parse("foo=bar\r\nbaz=qux");
    assertThat(map).hasSize(2);
    assertThat(map.get("foo")).isEqualTo("bar");
    assertThat(map.get("baz")).isEqualTo("qux");

    map = FakeAuthenticator.parse("foo=bar\nbaz=qux");
    assertThat(map).hasSize(2);
    assertThat(map.get("foo")).isEqualTo("bar");
    assertThat(map.get("baz")).isEqualTo("qux");

    map = FakeAuthenticator.parse("foo=bar\n\n\nbaz=qux");
    assertThat(map).hasSize(2);
    assertThat(map.get("foo")).isEqualTo("bar");
    assertThat(map.get("baz")).isEqualTo("qux");
  }

}
