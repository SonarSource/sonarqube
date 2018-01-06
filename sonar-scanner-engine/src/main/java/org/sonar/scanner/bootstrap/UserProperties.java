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
package org.sonar.scanner.bootstrap;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.config.Encryption;

/**
 * Properties that are coming from bootstrapper.
 */
public abstract class UserProperties {

  private final Map<String, String> properties;
  private final Encryption encryption;

  public UserProperties(Map<String, String> properties, @Nullable String pathToSecretKey) {
    encryption = new Encryption(pathToSecretKey);
    Map<String, String> decryptedProps = new HashMap<>(properties.size());
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      String value = entry.getValue();
      if (value != null && encryption.isEncrypted(value)) {
        try {
          value = encryption.decrypt(value);
        } catch (Exception e) {
          throw new IllegalStateException("Fail to decrypt the property " + entry.getKey() + ". Please check your secret key.", e);
        }
      }
      decryptedProps.put(entry.getKey(), value);
    }
    this.properties = decryptedProps;
  }

  public Encryption getEncryption() {
    return encryption;
  }

  public Map<String, String> properties() {
    return properties;
  }

  public String property(String key) {
    return properties.get(key);
  }

}
