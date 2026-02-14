/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.tokenprovider;

import java.util.Properties;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;

import static org.sonar.process.ProcessProperties.Property.JDBC_AWS_HOSTNAME;
import static org.sonar.process.ProcessProperties.Property.JDBC_AWS_PORT;
import static org.sonar.process.ProcessProperties.Property.JDBC_AWS_REGION;
import static org.sonar.process.ProcessProperties.Property.JDBC_USERNAME;

// https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html
public class AwsTokenProvider implements TokenProvider {
  @Override
  public String getToken(Properties properties) {

    String region = require(properties, JDBC_AWS_REGION.getKey());
    String hostName = require(properties, JDBC_AWS_HOSTNAME.getKey());
    String portStr = require(properties, JDBC_AWS_PORT.getKey());
    String username = require(properties, JDBC_USERNAME.getKey());

    int port;
    try {
      port = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid AWS port: " + portStr, e);
    }

    Region awsRegion = Region.of(region);

    RdsUtilities utilities = RdsUtilities.builder()
      .region(awsRegion)
      .credentialsProvider(DefaultCredentialsProvider.builder().build())
      .build();

    return utilities.generateAuthenticationToken(builder -> builder
      .hostname(hostName)
      .port(port)
      .username(username));
  }

  private String require(Properties properties, String key) {
    String value = properties.getProperty(key);

    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required property: " + key);
    }

    return value;
  }

}
