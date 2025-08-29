/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.process.ProcessProperties.Property;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section.Builder;
import org.sonar.server.platform.NodeInformation;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.lang3.Strings.CI;
import static org.sonar.process.ProcessProperties.Property.AUTH_JWT_SECRET;
import static org.sonar.process.ProcessProperties.Property.CE_JAVA_ADDITIONAL_OPTS;
import static org.sonar.process.ProcessProperties.Property.CE_JAVA_OPTS;
import static org.sonar.process.ProcessProperties.Property.SEARCH_JAVA_ADDITIONAL_OPTS;
import static org.sonar.process.ProcessProperties.Property.SEARCH_JAVA_OPTS;
import static org.sonar.process.ProcessProperties.Property.WEB_JAVA_ADDITIONAL_OPTS;
import static org.sonar.process.ProcessProperties.Property.WEB_JAVA_OPTS;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class SettingsSection implements SystemInfoSection, Global {
  private static final String PASSWORD_VALUE = "xxxxxxxx";
  private static final Collection<String> IGNORED_SETTINGS_IN_CLUSTER = Stream.of(
      WEB_JAVA_OPTS,
      WEB_JAVA_ADDITIONAL_OPTS,
      CE_JAVA_OPTS,
      CE_JAVA_ADDITIONAL_OPTS,
      SEARCH_JAVA_OPTS,
      SEARCH_JAVA_ADDITIONAL_OPTS)
    .map(Property::getKey)
    .collect(toUnmodifiableSet());

  private final DbClient dbClient;
  private final Settings settings;
  private final NodeInformation nodeInformation;

  public SettingsSection(DbClient dbClient, Settings settings, NodeInformation nodeInformation) {
    this.dbClient = dbClient;
    this.settings = settings;
    this.nodeInformation = nodeInformation;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Settings");

    PropertyDefinitions definitions = settings.getDefinitions();
    TreeMap<String, String> orderedProps = new TreeMap<>(settings.getProperties());
    orderedProps.entrySet()
      .stream()
      .filter(prop -> nodeInformation.isStandalone() || !IGNORED_SETTINGS_IN_CLUSTER.contains(prop.getKey()))
      .forEach(prop -> includeSetting(protobuf, definitions, prop));
    addDefaultNewCodeDefinition(protobuf);
    return protobuf.build();
  }

  private static void includeSetting(Builder protobuf, PropertyDefinitions definitions, Entry<String, String> prop) {
    String key = prop.getKey();
    String value = obfuscateValue(definitions, key, prop.getValue());
    setAttribute(protobuf, key, value);
  }

  private void addDefaultNewCodeDefinition(Builder protobuf) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<NewCodePeriodDto> period = dbClient.newCodePeriodDao().selectGlobal(dbSession);
      setAttribute(protobuf, "Default New Code Definition", parseDefaultNewCodeDefinition(period.orElse(NewCodePeriodDto.defaultInstance())));
    }
  }

  private static String obfuscateValue(PropertyDefinitions definitions, String key, String value) {
    PropertyDefinition def = definitions.get(key);
    if (def != null && def.type() == PropertyType.PASSWORD) {
      return PASSWORD_VALUE;
    }
    if (CI.endsWith(key, ".secured") ||
      CI.contains(key, "password") ||
      CI.contains(key, "passcode") ||
      AUTH_JWT_SECRET.getKey().equals(key)) {
      return PASSWORD_VALUE;
    }
    return value;
  }

  private static String parseDefaultNewCodeDefinition(NewCodePeriodDto period) {
    if (period.getValue() == null) {
      return period.getType().name();
    }

    return period.getType().name() + ": " + period.getValue();
  }
}
