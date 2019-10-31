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
package org.sonar.process;

import java.security.Permission;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class SecurityManagementTest {
  private ClassLoader classRealm = mock(ClassLoader.class, RETURNS_DEEP_STUBS);
  private ProtectionDomain pd = new ProtectionDomain(null, null, classRealm, null);

  private Permission allowedRuntime = new RuntimePermission("getFileSystemAttributes");
  private Permission deniedRuntime = new RuntimePermission("getClassLoader");
  private Permission allowedSecurity = new SecurityPermission("getProperty.key");
  private Permission deniedSecurity = new SecurityPermission("setPolicy");

  @Test
  public void policy_restricts_class_realm() {
    SecurityManagement.CustomPolicy policy = new SecurityManagement.CustomPolicy() {
      @Override
      String getDomainClassLoaderName(ProtectionDomain domain) {
        return "org.sonar.classloader.ClassRealm";
      }
    };

    assertThat(policy.implies(pd, allowedSecurity)).isTrue();
    assertThat(policy.implies(pd, deniedSecurity)).isFalse();
    assertThat(policy.implies(pd, allowedRuntime)).isTrue();
    assertThat(policy.implies(pd, deniedRuntime)).isFalse();
  }

  @Test
  public void policy_does_not_restrict_other_classloaders() {
    SecurityManagement.CustomPolicy policy = new SecurityManagement.CustomPolicy() {
      @Override
      String getDomainClassLoaderName(ProtectionDomain domain) {
        return "classloader";
      }
    };

    assertThat(policy.implies(pd, allowedSecurity)).isTrue();
    assertThat(policy.implies(pd, deniedSecurity)).isTrue();
    assertThat(policy.implies(pd, allowedRuntime)).isTrue();
    assertThat(policy.implies(pd, deniedRuntime)).isTrue();
  }
}
