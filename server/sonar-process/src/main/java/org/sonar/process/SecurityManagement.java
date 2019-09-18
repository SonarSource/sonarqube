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

import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecurityManagement {
  private SecurityManagement() {
    // static only
  }

  public static void restrictPlugins() {
    SecurityManager sm = new SecurityManager();
    Policy.setPolicy(new CustomPolicy());
    System.setSecurityManager(sm);
  }

  static class CustomPolicy extends Policy {
    private static final Set<String> ALLOWED_RUNTIME_PERMISSIONS = new HashSet<>(Arrays.asList("getFileSystemAttributes", "readFileDescriptor", "writeFileDescriptor",
      "getStackTrace", "setDefaultUncaughtExceptionHandler", "manageProcess", "localeServiceProvider", "LoggerFinder"));

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
      // classloader used to load plugins
      String clName = getDomainClassLoaderName(domain);
      if ("org.sonar.classloader.ClassRealm".equals(clName)) {
        if (permission instanceof RuntimePermission && !ALLOWED_RUNTIME_PERMISSIONS.contains(permission.getName())) {
          return false;
        }
        if (permission instanceof ReflectPermission || permission instanceof SecurityPermission) {
          return false;
        }
      }
      return true;
    }

    String getDomainClassLoaderName(ProtectionDomain domain) {
      return domain.getClassLoader().getClass().getName();
    }
  }
}
