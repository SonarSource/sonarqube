/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.scm.svn;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.MessageException;

@ScannerSide
public class SvnConfiguration {

  private static final String CATEGORY_SVN = "SVN";
  public static final String USER_PROP_KEY = "sonar.svn.username";
  public static final String PRIVATE_KEY_PATH_PROP_KEY = "sonar.svn.privateKeyPath";
  public static final String PASSWORD_PROP_KEY = "sonar.svn.password.secured";
  public static final String PASSPHRASE_PROP_KEY = "sonar.svn.passphrase.secured";
  private final Configuration config;

  public SvnConfiguration(Configuration config) {
    this.config = config;
  }

  public static List<PropertyDefinition> getProperties() {
    return Arrays.asList(
      PropertyDefinition.builder(USER_PROP_KEY)
        .name("Username")
        .description("Username to be used for SVN server or SVN+SSH authentication")
        .type(PropertyType.STRING)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_SCM)
        .subCategory(CATEGORY_SVN)
        .index(0)
        .build(),
      PropertyDefinition.builder(PASSWORD_PROP_KEY)
        .name("Password")
        .description("Password to be used for SVN server or SVN+SSH authentication")
        .type(PropertyType.PASSWORD)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_SCM)
        .subCategory(CATEGORY_SVN)
        .index(1)
        .build(),
      PropertyDefinition.builder(PRIVATE_KEY_PATH_PROP_KEY)
        .name("Path to private key file")
        .description("Can be used instead of password for SVN+SSH authentication")
        .type(PropertyType.STRING)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_SCM)
        .subCategory(CATEGORY_SVN)
        .index(2)
        .build(),
      PropertyDefinition.builder(PASSPHRASE_PROP_KEY)
        .name("Passphrase")
        .description("Optional passphrase of your private key file")
        .type(PropertyType.PASSWORD)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_SCM)
        .subCategory(CATEGORY_SVN)
        .index(3)
        .build());
  }

  @CheckForNull
  public String username() {
    return config.get(USER_PROP_KEY).orElse(null);
  }

  @CheckForNull
  public String password() {
    return config.get(PASSWORD_PROP_KEY).orElse(null);
  }

  @CheckForNull
  public File privateKey() {
    Optional<String> privateKeyOpt = config.get(PRIVATE_KEY_PATH_PROP_KEY);
    if (privateKeyOpt.isPresent()) {
      File privateKeyFile = new File(privateKeyOpt.get());
      if (!privateKeyFile.exists() || !privateKeyFile.isFile() || !privateKeyFile.canRead()) {
        throw MessageException.of("Unable to read private key from '" + privateKeyFile + "'");
      }
      return privateKeyFile;
    }
    return null;
  }

  @CheckForNull
  public String passPhrase() {
    return config.get(PASSPHRASE_PROP_KEY).orElse(null);
  }

}
