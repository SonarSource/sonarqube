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
package org.sonar.server.es;

import com.google.common.net.HostAndPort;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.common.settings.Settings;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.springframework.context.annotation.Bean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.sonar.process.ProcessProperties.Property.*;

@ComputeEngineSide
@ServerSide
public class EsClientProvider {
    private static final Logger LOGGER = Loggers.get(EsClientProvider.class);

    private static List<HttpHost> getHttpHosts(Configuration config) {
        return Arrays.stream(config.getStringArray(CLUSTER_SEARCH_HOSTS.getKey())).map(HostAndPort::fromString).map(EsClientProvider::toHttpHost).toList();
    }

    private static HttpHost toHttpHost(HostAndPort host) {
        try {
            return new HttpHost(InetAddress.getByName(host.getHost()), host.getPortOrDefault(9001));
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Can not resolve host [" + host + "]", e);
        }
    }

    private static String displayedAddresses(List<HttpHost> httpHosts) {
        return httpHosts.stream().map(HttpHost::toString).collect(Collectors.joining(", "));
    }

    @Bean("EsClient")
    public EsClient provide(Configuration config) {
        Settings.Builder esSettings = Settings.builder();

        // mandatory property defined by bootstrap process
        esSettings.put("cluster.name", config.get(CLUSTER_NAME.getKey()).get());

        List<HttpHost> httpHosts;

        if (StringUtils.isNotBlank(config.get(CLUSTER_SEARCH_HOSTS.getKey()).orElse(null))) {
            httpHosts = getHttpHosts(config);
            LOGGER.info("Connected to remote Elasticsearch: [{}]", displayedAddresses(httpHosts));
        } else {
            // defaults provided in:
            // * in org.sonar.process.ProcessProperties.Property.SEARCH_HOST
            // * in org.sonar.process.ProcessProperties.Property.SEARCH_PORT
            HostAndPort host = HostAndPort.fromParts(config.get(SEARCH_HOST.getKey()).get(), config.getInt(SEARCH_PORT.getKey()).get());
            httpHosts = Collections.singletonList(toHttpHost(host));
            LOGGER.info("Connected to local Elasticsearch: [{}]", displayedAddresses(httpHosts));
        }
        return new EsClient(config.get(CLUSTER_SEARCH_PASSWORD.getKey()).orElse(null), httpHosts.toArray(new HttpHost[0]));
    }
}
