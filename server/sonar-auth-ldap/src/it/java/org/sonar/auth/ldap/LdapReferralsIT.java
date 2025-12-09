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
package org.sonar.auth.ldap;

import java.util.Map;
import javax.annotation.Nullable;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.auth.ldap.server.LdapServer;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapReferralsIT {

  @ClassRule
  public static LdapServer server = new LdapServer("/users.example.org.ldif");

  Map<String, LdapContextFactory> underTest;

  @Test
  public void referral_is_set_to_follow_when_followReferrals_setting_is_set_to_true() {
    underTest = createFactories("ldap.followReferrals", "true");

    LdapContextFactory contextFactory = underTest.values().iterator().next();
    assertThat(contextFactory.getReferral()).isEqualTo("follow");
  }

  @Test
  public void referral_is_set_to_ignore_when_followReferrals_setting_is_set_to_false() {
    underTest = createFactories("ldap.followReferrals", "false");

    LdapContextFactory contextFactory = underTest.values().iterator().next();
    assertThat(contextFactory.getReferral()).isEqualTo("ignore");
  }

  @Test
  public void referral_is_set_to_follow_when_no_followReferrals_setting() {
    underTest = createFactories(null, null);

    LdapContextFactory contextFactory = underTest.values().iterator().next();
    assertThat(contextFactory.getReferral()).isEqualTo("follow");
  }

  private static Map<String, LdapContextFactory> createFactories(@Nullable String propertyKey, @Nullable String propertyValue) {
    MapSettings settings = LdapSettingsFactory.generateSimpleAnonymousAccessSettings(server, null);
    if (propertyKey != null) {
      settings.setProperty(propertyKey, propertyValue);
    }
    return new LdapSettingsManager(settings.asConfig()).getContextFactories();
  }
}
