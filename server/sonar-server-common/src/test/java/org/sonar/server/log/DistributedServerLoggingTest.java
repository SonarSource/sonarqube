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
package org.sonar.server.log;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.cp.IAtomicReference;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipOutputStream;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.Database;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.HazelcastMember;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.PROCESS_KEY;

public class DistributedServerLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final MapSettings settings = new MapSettings();
  private final ServerProcessLogging serverProcessLogging = mock();
  private final Database database = mock();
  private final HazelcastMember hazelcastMember = mock();
  private final OkHttpClient client = mock();

  private final DistributedServerLogging underTest = new DistributedServerLogging(settings.asConfig(), serverProcessLogging,
    database, hazelcastMember, client);

  private File dirWithLogs;

  @Before
  public void before() throws InterruptedException, IOException {
    Cluster cluster = mock();
    Member member1 = mock(), member2 = mock();
    Set<Member> members = Set.of(member1, member2);
    when(hazelcastMember.getCluster()).thenReturn(cluster);
    when(cluster.getMembers()).thenReturn(members);
    when(member1.getAttribute(PROCESS_KEY.getKey())).thenReturn(ProcessId.WEB_SERVER.getKey());
    when(member2.getAttribute(PROCESS_KEY.getKey())).thenReturn(ProcessId.WEB_SERVER.getKey());

    DistributedAnswer<Object> answer = mock();
    when(hazelcastMember.call(any(), any(), anyLong())).thenReturn(answer);
    when(answer.getSingleAnswer()).thenReturn(Optional.of(new DistributedServerLogging.WebAddress("anyhost", 9000)));
    when(hazelcastMember.getUuid()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    when(member1.getUuid()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    when(member2.getUuid()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    dirWithLogs = temp.newFolder();
    settings.setProperty(PATH_LOGS.getKey(), dirWithLogs.getAbsolutePath());
    settings.setProperty(ProcessProperties.Property.WEB_HOST.getKey(), "anyhost");

    IAtomicReference<Object> reference = mock();
    when(hazelcastMember.getAtomicReference("AUTH_SECRET")).thenReturn(reference);

    underTest.start();
  }

  @Test
  public void productionConstructor_doesNotThrowException() {
    DistributedServerLogging distributedServerLogging = new DistributedServerLogging(settings.asConfig(),
      serverProcessLogging, database, hazelcastMember);

    assertThat(distributedServerLogging).isNotNull();
  }

  @Test
  public void isValidNodeToNodeCall_whenNodeToNodeSecretIsInvalid() {
    IAtomicReference<Object> reference = mock();
    when(hazelcastMember.getAtomicReference("AUTH_SECRET")).thenReturn(reference);
    when(reference.get()).thenReturn("");

    boolean result = underTest.isValidNodeToNodeCall(Map.of("node_to_node_secret", "secret"));

    assertThat(result).isFalse();
  }

  @Test
  public void isValidNodeToNodeCall_whenNodeToNodeSecretIsValid() {
    when(hazelcastMember.getReplicatedMap("SECRETS")).thenReturn(Map.of("AUTH_SECRET", "secret"));

    boolean result = underTest.isValidNodeToNodeCall(Map.of("node_to_node_secret", "secret"));

    assertThat(result).isTrue();
  }

  @Test
  public void addLogFromResponseToZip_whenSourceIsNotEmpty_writeToOutputStream() throws IOException {
    ZipOutputStream zipOutputStream = mock();
    Response response = mock();
    ResponseBody responseBody = mock();
    when(response.body()).thenReturn(responseBody);
    when(responseBody.source()).thenReturn(new Buffer().writeUtf8("response_body"));

    DistributedServerLogging.addLogFromResponseToZip(response, zipOutputStream);

    verify(zipOutputStream, atLeastOnce()).write(any(byte[].class));
  }

  @Test
  public void getDistributedLogs_whenRetrieveWebAddressOfAMemberFails_throwException() throws InterruptedException {
    when(hazelcastMember.call(any(), any(), anyLong())).thenThrow(new InterruptedException());

    assertThatThrownBy(() -> underTest.getDistributedLogs("ce", "ce"))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void getDistributedLogs_whenHttpCallFails_throwException() throws InterruptedException {
    when(hazelcastMember.call(any(), any(), anyLong())).thenThrow(new InterruptedException());

    assertThatThrownBy(() -> underTest.getDistributedLogs("ce", "ce"))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getDistributedLogs_whenAnswerFromNodeIsEmpty_throwException() throws InterruptedException {
    DistributedAnswer<Object> answer = mock();
    when(hazelcastMember.call(any(), any(), anyLong())).thenReturn(answer);
    when(answer.getSingleAnswer()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getDistributedLogs("ce", "ce"))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getDistributedLogs_whenHttpCallPasses_returnFile() throws IOException {
    Call call = mock();
    Response response = mock();
    ResponseBody responseBody = mock();
    when(client.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(response);
    when(response.body()).thenReturn(responseBody);
    when(responseBody.source()).thenReturn(new Buffer());

    FileUtils.write(new File(dirWithLogs, "ce.log"), "ce_logs_data", Charset.defaultCharset());

    File distributedLogs = underTest.getDistributedLogs("ce", "ce");

    assertThat(distributedLogs).isNotNull();
  }
}
