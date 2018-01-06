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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Settings;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.UserDetails;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@Properties({
  @Property(
    key = FakeAuthenticator.DATA_PROPERTY,
    name = "Fake Users", type = PropertyType.TEXT
  )
})
public class FakeAuthenticator implements LoginPasswordAuthenticator {

  private static final Logger LOG = Loggers.get(FakeAuthenticator.class);

  /**
   * Example:
   * <pre>
   * evgeny.password=foo
   * evgeny.name=Evgeny Mandrikov
   * evgeny.email=evgeny@example.org
   * evgeny.groups=sonar-users
   *
   * simon.password=bar
   * simon.groups=sonar-users,sonar-developers
   * </pre>
   */
  public static final String DATA_PROPERTY = "sonar.fakeauthenticator.users";

  private final Settings settings;

  private Map<String, String> data;

  public FakeAuthenticator(Settings settings) {
    this.settings = settings;
  }

  public boolean authenticate(String username, String password) {
    // Never touch admin
    if (isAdmin(username)) {
      return true;
    }

    reloadData();
    checkExistence(username);

    String expectedPassword = data.get(username + ".password");
    if (StringUtils.equals(password, expectedPassword)) {
      LOG.info("user {} with password {}", username, password);
      return true;
    } else {
      LOG.info("user " + username + " expected password " + expectedPassword + " , but was " + password);
      return false;
    }
  }

  private void checkExistence(String username) {
    if (!data.containsKey(username + ".password")) {
      throw new IllegalArgumentException("No such user : " + username);
    }
  }

  public UserDetails doGetUserDetails(String username) {
    // Never touch admin
    if (isAdmin(username)) {
      return null;
    }

    reloadData();
    checkExistence(username);

    UserDetails result = new UserDetails();
    result.setName(Strings.nullToEmpty(data.get(username + ".name")));
    result.setEmail(Strings.nullToEmpty(data.get(username + ".email")));
    LOG.info("details for user {} : {}", username, result);
    return result;
  }

  public Collection<String> doGetGroups(String username) {
    // Never touch admin
    if (isAdmin(username)) {
      return null;
    }

    reloadData();
    checkExistence(username);

    Collection<String> result = parseList(data.get(username + ".groups"));
    LOG.info("groups for user {} : {}", username, result);
    return result;
  }

  private static boolean isAdmin(String username) {
    return StringUtils.equals(username, "admin");
  }

  private void reloadData() {
    data = parse(settings.getString(DATA_PROPERTY));
  }

  private static final Splitter LIST_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Splitter LINE_SPLITTER = Splitter.on(Pattern.compile("\r?\n")).omitEmptyStrings().trimResults();

  @VisibleForTesting
  static List<String> parseList(String data) {
    return ImmutableList.copyOf(LIST_SPLITTER.split(Strings.nullToEmpty(data)));
  }

  @VisibleForTesting
  static Map<String, String> parse(String data) {
    Map<String, String> result = Maps.newHashMap();
    for (String entry : LINE_SPLITTER.split(Strings.nullToEmpty(data))) {
      Iterator<String> keyValue = Splitter.on('=').split(entry).iterator();
      result.put(keyValue.next(), keyValue.next());
    }
    return result;
  }

  public void init() {
    // nothing to do
  }

}
