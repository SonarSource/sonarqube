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
package org.sonar.server.log;

import com.google.common.annotations.VisibleForTesting;
import com.hazelcast.cluster.Member;
import jakarta.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.Database;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.DistributedCall;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberSelectors;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.PROCESS_KEY;
import static org.sonar.process.cluster.hz.HazelcastObjects.AUTH_SECRET;
import static org.sonar.process.cluster.hz.HazelcastObjects.LOG_LEVEL_KEY;
import static org.sonar.process.cluster.hz.HazelcastObjects.RUNTIME_CONFIG;
import static org.sonar.process.cluster.hz.HazelcastObjects.SECRETS;

public class DistributedServerLogging extends ServerLogging {

  public static final String NODE_TO_NODE_SECRET = "node_to_node_secret";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Logger LOGGER = LoggerFactory.getLogger(DistributedServerLogging.class);
  private final HazelcastMember hazelcastMember;
  private final OkHttpClient client;

  @Inject
  public DistributedServerLogging(Configuration config, ServerProcessLogging serverProcessLogging, Database database, HazelcastMember hazelcastMember) {
    super(config, serverProcessLogging, database);
    this.hazelcastMember = hazelcastMember;
    this.client = new OkHttpClientBuilder()
      .setConnectTimeoutMs(30000)
      .setReadTimeoutMs(30000)
      .setFollowRedirects(false)
      .build();
  }

  @VisibleForTesting
  public DistributedServerLogging(Configuration config, ServerProcessLogging serverProcessLogging, Database database, HazelcastMember hazelcastMember, OkHttpClient client) {
    super(config, serverProcessLogging, database);
    this.hazelcastMember = hazelcastMember;
    this.client = client;
  }

  @Override
  public void start() {
    super.start();
    loadLogLevelFromHazelcast();
  }

  private void loadLogLevelFromHazelcast() {
    try {
      Map<String, String> runtimeConfig = hazelcastMember.getReplicatedMap(RUNTIME_CONFIG);
      final String logLevelStr = runtimeConfig.get(LOG_LEVEL_KEY);
      if (logLevelStr != null) {
        final LoggerLevel level = LoggerLevel.valueOf(logLevelStr);
        changeLevel(level);
        LOGGER.debug("Applied runtime log level '{}' from Hazelcast", level);
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to load log level from Hazelcast", e);
    }
  }

  private WebAddress retrieveWebAddressOfAMember(Member member) {
    try {
      DistributedAnswer<WebAddress> answerWithAddress = hazelcastMember.call(askForWebAPIAddress(),
        HazelcastMemberSelectors.selectorForMember(member), 5000L);
      Optional<WebAddress> singleAnswer = answerWithAddress.getSingleAnswer();
      if (singleAnswer.isEmpty()) {
        throw new IllegalStateException("No web API address found for member with UUID " + member.getUuid());
      }
      return singleAnswer.get();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean isValidNodeToNodeCall(Map<String, String> headers) {
    String requestSecret = headers.get(NODE_TO_NODE_SECRET);
    if (requestSecret == null || requestSecret.isEmpty()) {
      return false;
    }
    Object savedSecret = tryGetSecretFromMap();
    return Objects.equals(savedSecret, requestSecret);
  }

  private Object tryGetSecretFromMap() {
    final int maxRetries = 5;
    for (int i = 0; i < maxRetries; i++) {
      Map<Object, Object> secrets = hazelcastMember.getReplicatedMap(SECRETS);
      if (!secrets.containsKey(AUTH_SECRET)) {
        sleep(2000);
        continue;
      }
      return secrets.get(AUTH_SECRET);
    }
    return null;
  }

  private static void sleep(int miliseconds) {
    try {
      Thread.sleep(miliseconds);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String generateOneTimeToken() {
    byte[] randomBytes = new byte[20];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Hex.encodeHexString(randomBytes);
  }

  @Override
  public File getDistributedLogs(String filePrefix, String logName) {
    File zipFile = createZipFile();
    if (filePrefix.equals(ProcessId.ELASTICSEARCH.getLogFilenamePrefix())) {
      // Elasticsearch logs are not distributed, something to implement in the future
      throw new IllegalArgumentException("Elasticsearch logs are not distributed");
    }

    String oneTimeToken = generateOneTimeToken();
    setHazelcastAuthSecret(oneTimeToken);
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
      Set<Member> members = findMembersWithLogs();
      WebAddress localWebAPIAddress = askForWebAPIAddress().call();

      // add data from other nodes
      for (Member member : members) {
        processMember(filePrefix, logName, member, localWebAPIAddress, oneTimeToken, out);
      }

      // and add data from the current node
      out.putNextEntry(createZipEntry(filePrefix, hazelcastMember.getUuid().toString()));
      FileUtils.copyFile(getLogsForSingleNode(filePrefix), out);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to collect logs from the nodes", e);
    } finally {
      resetHazelcastAuthSecret();
    }
    return zipFile;
  }

  private static File createZipFile() {
    try {
      Path tempDir = Files.createTempDirectory("logs");
      return new File(tempDir.toFile(), "logs.zip");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @NotNull
  private Set<Member> findMembersWithLogs() {
    return hazelcastMember.getCluster().getMembers().stream()
      .filter(member -> member.getAttribute(PROCESS_KEY.getKey()).equals(ProcessId.WEB_SERVER.getKey()))
      .collect(Collectors.toSet());
  }

  private void processMember(String filePrefix, String logName, Member member, WebAddress localWebAPIAddress,
    String oneTimeToken, ZipOutputStream out) throws IOException {
    WebAddress addressAndPort = retrieveWebAddressOfAMember(member);
    String address = addressAndPort.address;

    String context = addressAndPort.context;
    int port = addressAndPort.port;
    if (address.equals(localWebAPIAddress.address) && port == localWebAPIAddress.port) {
      return;
    }

    String url = String.format("http://%s:%d%s/api/system/logs?name=%s", address, port, context, logName);
    Request request = new Request.Builder().url(url)
      .addHeader(NODE_TO_NODE_SECRET, oneTimeToken)
      .build();

    try (Response response = client.newCall(request).execute()) {
      out.putNextEntry(createZipEntry(filePrefix, member.getUuid().toString()));
      addLogFromResponseToZip(response, out);
    }
  }

  @VisibleForTesting
  static void addLogFromResponseToZip(Response response, ZipOutputStream out) throws IOException {
    BufferedSource source = response.body().source();
    try (Buffer buffer = new Buffer()) {
      while (!source.exhausted()) {
        // we read up to 1MB at a time
        long count = source.read(buffer, 1_048_576);
        byte[] buf = new byte[(int) count];
        buffer.read(buf, 0, (int) count);
        out.write(buf);
      }
    }
  }

  private static ZipEntry createZipEntry(String filePrefix, String uuid) {
    return new ZipEntry(filePrefix + "-" + uuid + ".log");
  }

  private void setHazelcastAuthSecret(String someSecret) {
    hazelcastMember.getReplicatedMap(SECRETS).putIfAbsent(AUTH_SECRET, someSecret);
  }

  private void resetHazelcastAuthSecret() {
    hazelcastMember.getReplicatedMap(SECRETS).remove(AUTH_SECRET);
  }

  private static DistributedCall<WebAddress> askForWebAPIAddress() {
    return () -> new WebAddress(ServerLogging.getWebAPIAddressFromHazelcastQuery(), ServerLogging.getWebAPIContextFromHazelcastQuery(),
      ServerLogging.getWebAPIPortFromHazelcastQuery());
  }

  public record WebAddress(String address, String context, int port) implements Serializable {
  }
}
