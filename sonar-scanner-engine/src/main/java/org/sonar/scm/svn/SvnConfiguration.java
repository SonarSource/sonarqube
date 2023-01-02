/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;

import static org.sonar.core.config.SvnProperties.PASSPHRASE_PROP_KEY;
import static org.sonar.core.config.SvnProperties.PASSWORD_PROP_KEY;
import static org.sonar.core.config.SvnProperties.PRIVATE_KEY_PATH_PROP_KEY;
import static org.sonar.core.config.SvnProperties.USER_PROP_KEY;

public class SvnConfiguration {
  private final Configuration config;

  public SvnConfiguration(Configuration config) {
    this.config = config;
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

  public boolean isEmpty() {
    return username() == null && password() == null && privateKey() == null && passPhrase() == null;
  }
}
