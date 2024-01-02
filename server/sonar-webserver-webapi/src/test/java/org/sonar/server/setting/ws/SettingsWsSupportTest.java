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
package org.sonar.server.setting.ws;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(value = Parameterized.class)
public class SettingsWsSupportTest {

  private static final String KEY_REQUIRING_ADMIN_PERMISSION = "sonar.auth.bitbucket.workspaces";
  private static final String STANDARD_KEY = "sonar.auth.bitbucket.enabled";
  private static final String SECURED_KEY = "sonar.auth.bitbucket" + SettingsWsSupport.DOT_SECURED;

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      // admin cases
      {testCaseBuilderForAdminOnlyKey().isAdmin(true).expectedIsVisible(true).build()},
      {testCaseBuilderForStandardKey().isAdmin(true).expectedIsVisible(true).build()},
      {testCaseBuilderForSecuredKey().isAdmin(true).expectedIsVisible(true).build()},

      // non-admin cases
      {testCaseBuilderForAdminOnlyKey().isAdmin(false).hasGlobalPermission(true).hasComponentPermission(true).expectedIsVisible(true).build()},
      {testCaseBuilderForAdminOnlyKey().isAdmin(false).hasGlobalPermission(true).hasComponentPermission(false).expectedIsVisible(true).build()},
      {testCaseBuilderForAdminOnlyKey().isAdmin(false).hasGlobalPermission(false).hasComponentPermission(true).expectedIsVisible(true).build()},
      {testCaseBuilderForAdminOnlyKey().isAdmin(false).hasGlobalPermission(false).hasComponentPermission(false).expectedIsVisible(false).build()},
      {testCaseBuilderForSecuredKey().isAdmin(false).hasGlobalPermission(true).hasComponentPermission(true).expectedIsVisible(true).build()},
      {testCaseBuilderForSecuredKey().isAdmin(false).hasGlobalPermission(true).hasComponentPermission(false).expectedIsVisible(true).build()},
      {testCaseBuilderForSecuredKey().isAdmin(false).hasGlobalPermission(false).hasComponentPermission(true).expectedIsVisible(true).build()},
      {testCaseBuilderForSecuredKey().isAdmin(false).hasGlobalPermission(false).hasComponentPermission(false).expectedIsVisible(false).build()},
      {testCaseBuilderForStandardKey().isAdmin(false).hasGlobalPermission(false).hasComponentPermission(false).expectedIsVisible(true).build()},
      {testCaseBuilderForStandardKey().isAdmin(false).hasGlobalPermission(false).hasComponentPermission(true).expectedIsVisible(true).build()},
      {testCaseBuilderForStandardKey().isAdmin(false).hasGlobalPermission(true).hasComponentPermission(true).expectedIsVisible(true).build()},
      {testCaseBuilderForStandardKey().isAdmin(false).hasGlobalPermission(true).hasComponentPermission(false).expectedIsVisible(true).build()},
    });
  }
  
  private final boolean isAdmin;
  private final String property;
  private final boolean hasGlobalPermission;
  private final boolean hasComponentPermission;
  private final boolean expectedIsVisible;

  @Mock
  private ComponentDto componentDto;
  @Mock
  private UserSession userSession;
  @InjectMocks
  private SettingsWsSupport settingsWsSupport;

  public SettingsWsSupportTest(TestCase testCase) {
    this.isAdmin = testCase.isAdmin;
    this.property = testCase.property;
    this.hasGlobalPermission = testCase.hasGlobalPermission;
    this.hasComponentPermission = testCase.hasComponentPermission;
    this.expectedIsVisible = testCase.expectedIsVisible;
  }

  @Test
  public void isVisible() {
    openMocks(this);
    when(userSession.isSystemAdministrator()).thenReturn(isAdmin);
    when(userSession.hasPermission(GlobalPermission.SCAN)).thenReturn(hasGlobalPermission);
    when(userSession.hasComponentPermission(UserRole.SCAN, componentDto)).thenReturn(hasComponentPermission);

    boolean isVisible = settingsWsSupport.isVisible(property, Optional.of(componentDto));
    assertThat(isVisible).isEqualTo(expectedIsVisible);
  }

  private static TestCase.TestCaseBuilder testCaseBuilderForAdminOnlyKey() {
    return testCaseBuilder().property(KEY_REQUIRING_ADMIN_PERMISSION);
  }

  private static TestCase.TestCaseBuilder testCaseBuilderForSecuredKey() {
    return testCaseBuilder().property(SECURED_KEY);
  }

  private static TestCase.TestCaseBuilder testCaseBuilderForStandardKey() {
    return testCaseBuilder().property(STANDARD_KEY);
  }

  private static TestCase.TestCaseBuilder testCaseBuilder() {
    return new TestCase.TestCaseBuilder();
  }  

  static final class TestCase {
    private final boolean isAdmin;
    private final String property;
    private final boolean hasGlobalPermission;
    private final boolean hasComponentPermission;
    private final boolean expectedIsVisible;

    TestCase(boolean isAdmin, String property, boolean hasGlobalPermission, boolean hasComponentPermission, boolean expectedIsVisible) {
      this.isAdmin = isAdmin;
      this.property = property;
      this.hasGlobalPermission = hasGlobalPermission;
      this.hasComponentPermission = hasComponentPermission;
      this.expectedIsVisible = expectedIsVisible;
    }

    @Override public String toString() {
      return new StringJoiner(", ", TestCase.class.getSimpleName() + "[", "]")
        .add("isAdmin=" + isAdmin)
        .add("property='" + property + "'")
        .add("hasComponentPermission=" + hasComponentPermission)
        .add("hasGlobalPermission=" + hasGlobalPermission)
        .add("expectedIsVisible=" + expectedIsVisible)
        .toString();
    }

    static final class TestCaseBuilder {
      private boolean isAdmin = false;
      private String property;
      private boolean hasGlobalPermission = false;
      private boolean hasComponentPermission = false;
      private boolean expectedIsVisible;

      TestCaseBuilder isAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        return this;
      }

      TestCaseBuilder property(String property) {
        this.property = property;
        return this;
      }

      TestCaseBuilder hasGlobalPermission(boolean hasGlobalPermission) {
        this.hasGlobalPermission = hasGlobalPermission;
        return this;
      }

      TestCaseBuilder hasComponentPermission(boolean hasComponentPermission) {
        this.hasComponentPermission = hasComponentPermission;
        return this;
      }

      TestCaseBuilder expectedIsVisible(boolean expectedIsVisible) {
        this.expectedIsVisible = expectedIsVisible;
        return this;
      }

      TestCase build() {
        return new TestCase(isAdmin, property, hasGlobalPermission, hasComponentPermission, expectedIsVisible);
      }
    }
  }

}
