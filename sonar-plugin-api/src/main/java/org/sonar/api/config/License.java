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
package org.sonar.api.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.DateUtils;

/**
 * SonarSource license. This class aims to extract metadata but not to validate or - of course -
 * to generate license
 *
 * @since 3.0
 */
public final class License {
  private String product;
  private String organization;
  private String expirationDate;
  private String type;
  private String server;
  private Map<String, String> additionalProperties;

  private License(Map<String, String> properties) {
    this.additionalProperties = new HashMap<>(properties);
    product = StringUtils.defaultString(get("Product", properties), get("Plugin", properties));
    organization = StringUtils.defaultString(get("Organisation", properties), get("Name", properties));
    expirationDate = StringUtils.defaultString(get("Expiration", properties), get("Expires", properties));
    type = get("Type", properties);
    server = get("Server", properties);
    // SONAR-4340 Don't expose Digest and Obeo properties
    additionalProperties.remove("Digest");
    additionalProperties.remove("Obeo");
  }

  private String get(String key, Map<String, String> properties) {
    additionalProperties.remove(key);
    return properties.get(key);
  }

  /**
   * Get additional properties available on this license (like threshold conditions)
   * @since 3.6
   */
  public Map<String, String> additionalProperties() {
    return additionalProperties;
  }

  @Nullable
  public String getProduct() {
    return product;
  }

  @Nullable
  public String getOrganization() {
    return organization;
  }

  @Nullable
  public String getExpirationDateAsString() {
    return expirationDate;
  }

  @Nullable
  public Date getExpirationDate() {
    return DateUtils.parseDateQuietly(expirationDate);
  }

  public boolean isExpired() {
    return isExpired(new Date());
  }

  boolean isExpired(Date now) {
    Date date = getExpirationDate();
    if (date == null) {
      return false;
    }
    // SONAR-6079 include last day
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.DAY_OF_MONTH, 1);
    cal.add(Calendar.SECOND, -1);
    return now.after(cal.getTime());
  }

  @Nullable
  public String getType() {
    return type;
  }

  @Nullable
  public String getServer() {
    return server;
  }

  public static License readBase64(String base64) {
    return readPlainText(new String(Base64.decodeBase64(base64.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
  }

  static License readPlainText(String data) {
    Map<String, String> props = new HashMap<>();
    try {
      List<String> lines = IOUtils.readLines(new StringReader(data));
      for (String line : lines) {
        if (StringUtils.isNotBlank(line) && line.indexOf(':') > 0) {
          String key = StringUtils.substringBefore(line, ":");
          String value = StringUtils.substringAfter(line, ":");
          props.put(StringUtils.trimToEmpty(key), StringUtils.trimToEmpty(value));
        }
      }

    } catch (IOException e) {
      // silently ignore
    }
    return new License(props);
  }
}
