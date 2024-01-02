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
package org.sonar.process;

import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import javax.management.MBeanPermission;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PluginSecurityManagerTest {
  private final ClassLoader classRealm = mock(ClassLoader.class, RETURNS_DEEP_STUBS);
  private final ProtectionDomain pd = new ProtectionDomain(null, null, classRealm, null);
  private final Permission permission = mock(Permission.class);
  private final PluginPolicyRule rule1 = mock(PluginPolicyRule.class);
  private final PluginPolicyRule rule2 = mock(PluginPolicyRule.class);

  @Test
  public void constructor_dontSetAnyPolicy() {
    Policy policy = Policy.getPolicy();

    new PluginSecurityManager();

    assertThat(policy).isEqualTo(Policy.getPolicy());
  }

  @Test
  public void protection_domain_can_have_no_classloader() {
    PluginSecurityManager.PluginPolicy policy = new PluginSecurityManager.PluginPolicy(Arrays.asList(rule1, rule2));

    ProtectionDomain domain = new ProtectionDomain(null, null, null, null);
    Permission permission = new MBeanPermission("com.sun.management.internal.HotSpotThreadImpl", "getMBeanInfo");

    assertThat(policy.implies(domain, permission)).isTrue();
    verifyNoInteractions(rule1, rule2);
  }

  @Test
  public void policy_doesnt_restrict_other_classloaders() {
    PluginSecurityManager.PluginPolicy policy = new PluginSecurityManager.PluginPolicy(Arrays.asList(rule1, rule2)) {
      @Override
      String getDomainClassLoaderName(ProtectionDomain domain) {
        return "classloader";
      }
    };

    policy.implies(pd, permission);
    verifyNoInteractions(rule1, rule2);
  }

  @Test
  public void policy_restricts_class_realm_classloader() {
    when(rule1.implies(permission)).thenReturn(true);
    PluginSecurityManager.PluginPolicy policy = new PluginSecurityManager.PluginPolicy(Arrays.asList(rule1, rule2)) {
      @Override
      String getDomainClassLoaderName(ProtectionDomain domain) {
        return "org.sonar.classloader.ClassRealm";
      }
    };

    policy.implies(pd, permission);
    verify(rule1).implies(permission);
    verify(rule2).implies(permission);
    verifyNoMoreInteractions(rule1, rule2);
  }

}
