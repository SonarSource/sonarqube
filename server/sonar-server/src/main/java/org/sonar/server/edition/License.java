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
package org.sonar.server.edition;

import com.google.common.collect.ImmutableSet;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class License {
  private static final Logger LOG = Loggers.get(License.class);
  private static final String EDITION_KEY = "Edition";
  private static final String PLUGINS_KEY = "Plugins";

  private final String editionKey;
  private final Set<String> pluginKeys;
  private final String content;

  public License(String editionKey, Collection<String> pluginKeys, String content) {
    this.editionKey = enforceNotNullNorEmpty(editionKey, "editionKey");
    this.pluginKeys = ImmutableSet.copyOf(pluginKeys);
    this.content = enforceNotNullNorEmpty(content, "content");
  }

  private static String enforceNotNullNorEmpty(String str, String propertyName) {
    checkNotNull(str, "%s can't be null", propertyName);
    checkArgument(!str.isEmpty(), "%s can't be empty", propertyName);
    return str;
  }

  public String getEditionKey() {
    return editionKey;
  }

  public Set<String> getPluginKeys() {
    return pluginKeys;
  }

  public String getContent() {
    return content;
  }

  public static Optional<License> parse(String base64) {
    try {
      String data = new String(Base64.decodeBase64(base64.trim().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

      Properties props = new Properties();
      props.load(new StringReader(data));

      String[] plugins = StringUtils.split(props.getProperty(PLUGINS_KEY), ',');
      String editionKey = props.getProperty(EDITION_KEY);

      if (StringUtils.isNotBlank(editionKey) && plugins.length > 0) {
        return Optional.of(new License(editionKey, Arrays.asList(plugins), base64));
      } else {
        LOG.debug("Failed to parse license: no edition key and/or no plugin found");
      }
    } catch (Exception e) {
      LOG.debug("Failed to parse license", e);
    }
    return Optional.empty();

  }
}
