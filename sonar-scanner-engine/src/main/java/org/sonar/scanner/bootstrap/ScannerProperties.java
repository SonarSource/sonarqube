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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.api.config.Encryption;

import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * Properties that are coming from scanner.
 */
@Immutable
public class ScannerProperties implements ProjectKey {

  private final Map<String, String> properties;
  private final Encryption encryption;

  public ScannerProperties(Map<String, String> properties) {
    encryption = new Encryption(properties.get(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
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
    return ImmutableMap.copyOf(properties);
  }

  public String property(String key) {
    return properties.get(key);
  }

  @Override
  public String get() {
    return getKeyWithBranch();
  }

  private String getKey() {
    return properties.get(CoreProperties.PROJECT_KEY_PROPERTY);
  }

  public String getKeyWithBranch() {
    String branch = getBranch();
    String projectKey = getKey();
    if (branch == null) {
      return projectKey;
    }
    return String.format("%s:%s", projectKey, branch);
  }

  @CheckForNull
  private String getBranch() {
    return trimToNull(properties.get(CoreProperties.PROJECT_BRANCH_PROPERTY));
  }
}
