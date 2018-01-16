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
package org.sonar.scanner.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.trim;
import static org.sonar.core.config.MultivalueProperty.parseAsCsv;

@Immutable
public abstract class DefaultConfiguration implements Configuration {

  private static final Logger LOG = Loggers.get(DefaultConfiguration.class);

  private final PropertyDefinitions definitions;
  private final Encryption encryption;
  private final GlobalAnalysisMode mode;
  private final Map<String, String> properties;

  public DefaultConfiguration(PropertyDefinitions propertyDefinitions, Encryption encryption, GlobalAnalysisMode mode, Map<String, String> props) {
    this.definitions = requireNonNull(propertyDefinitions);
    this.encryption = encryption;
    this.mode = mode;
    this.properties = unmodifiableMapWithTrimmedValues(definitions, props);
  }

  protected static Map<String, String> unmodifiableMapWithTrimmedValues(PropertyDefinitions definitions, Map<String, String> props) {
    Map<String, String> map = new HashMap<>(props.size());
    props.forEach((k, v) -> {
      String validKey = definitions.validKey(k);
      map.put(validKey, trim(v));
    });
    return Collections.unmodifiableMap(map);
  }

  public GlobalAnalysisMode getMode() {
    return mode;
  }

  public Encryption getEncryption() {
    return encryption;
  }

  public PropertyDefinitions getDefinitions() {
    return definitions;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public boolean hasKey(String key) {
    return properties.containsKey(key);
  }

  @Override
  public Optional<String> get(String key) {
    String effectiveKey = definitions.validKey(key);
    PropertyDefinition def = definitions.get(effectiveKey);
    if (def != null && (def.multiValues() || !def.fields().isEmpty())) {
      LOG.warn("Access to the multi-values/property set property '{}' should be made using 'getStringArray' method. The SonarQube plugin using this property should be updated.",
        key);
    }
    return getInternal(effectiveKey);
  }

  @Override
  public String[] getStringArray(String key) {
    String effectiveKey = definitions.validKey(key);
    PropertyDefinition def = definitions.get(effectiveKey);
    if (def != null && !def.multiValues() && def.fields().isEmpty()) {
      LOG.warn(
        "Property '{}' is not declared as multi-values/property set but was read using 'getStringArray' method. The SonarQube plugin declaring this property should be updated.",
        key);
    }
    Optional<String> value = getInternal(effectiveKey);
    if (value.isPresent()) {
      return parseAsCsv(effectiveKey, value.get());
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  private Optional<String> getInternal(String key) {
    if (mode.isIssues() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    }
    Optional<String> value = Optional.ofNullable(properties.get(key));
    if (!value.isPresent()) {
      // default values cannot be encrypted, so return value as-is.
      return Optional.ofNullable(definitions.getDefaultValue(key));
    }
    if (encryption.isEncrypted(value.get())) {
      try {
        return Optional.of(encryption.decrypt(value.get()));
      } catch (Exception e) {
        throw new IllegalStateException("Fail to decrypt the property " + key + ". Please check your secret key.", e);
      }
    }
    return value;
  }

}
