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
package org.sonar.server.platform.ws;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
import org.sonar.api.config.Configuration;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.Database;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.log.DistributedServerLogging;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

public class LogsActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private static final String DEFAULT_SECRET = "secret";

  private final Configuration configuration = mock();
  private final Database database = mock();
  private final ServerLogging serverLogging = new ServerLogging(configuration, mock(), database);
  private final PlatformEditionProvider editionProvider = mock();
  private final LogsAction underTest = new LogsAction(userSession, serverLogging, editionProvider);
  private final WsActionTester actionTester = new WsActionTester(underTest);

  private final OkHttpClient client = mock();
  private final HazelcastMember hazelcastMember = mock();
  private final DistributedServerLogging distributedServerLogging = new DistributedServerLogging(configuration, mock(), database, hazelcastMember, client);
  private final PlatformEditionProvider editionProviderDataCenter = mock();
  private final LogsAction underTestDataCenter = new LogsAction(userSession, distributedServerLogging, editionProviderDataCenter);
  private final WsActionTester actionTesterDataCenter = new WsActionTester(underTestDataCenter);

  @Before
  public void before() throws InterruptedException, IOException {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    when(editionProviderDataCenter.get()).thenReturn(Optional.of(EditionProvider.Edition.DATACENTER));

    Cluster cluster = mock();
    Member member1 = mock(), member2 = mock();
    Set<Member> members = Set.of(member1, member2);
    Map<Object, Object> mapWithSecret = Map.of("AUTH_SECRET", DEFAULT_SECRET);
    Call call = mock();
    DistributedAnswer answer = mock();
    Response response = mock();
    ResponseBody responseBody = mock();
    Buffer buffer = new Buffer();
    buffer.readFrom(new ByteArrayInputStream("exampleLog".getBytes(StandardCharsets.UTF_8)));
    when(member1.getAttribute(any())).thenReturn(ProcessId.WEB_SERVER.getKey());
    when(member2.getAttribute(any())).thenReturn(ProcessId.WEB_SERVER.getKey());
    when(hazelcastMember.getReplicatedMap("SECRET")).thenReturn(mapWithSecret);
    when(hazelcastMember.getCluster()).thenReturn(cluster);
    when(cluster.getMembers()).thenReturn(members);
    when(configuration.get(ProcessProperties.Property.WEB_HOST.getKey())).thenReturn(Optional.of("anyhost"));
    when(hazelcastMember.call(any(), any(), anyLong())).thenReturn(answer);
    when(answer.getSingleAnswer()).thenReturn(Optional.of(new DistributedServerLogging.WebAddress("anyhost", "/any", 9000)));
    when(hazelcastMember.getUuid()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    when(member1.getUuid()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    when(member2.getUuid()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    when(client.newCall(any())).thenReturn(call);
    when(response.body()).thenReturn(responseBody);
    when(responseBody.source()).thenReturn(buffer);
    when(call.execute()).thenReturn(response);
    distributedServerLogging.start();
  }

  // values are lower-case and alphabetically ordered
  @Test
  public void possibleValues_shouldReturnPossibleLogFileValues() {
    Set<String> values = actionTester.getDef().param("name").possibleValues();
    assertThat(values).containsExactly("access", "app", "ce", "deprecation", "es", "web");
  }

  @Test
  public void execute_whenUserNotLoggedIn_shouldFailWithForbiddenException() {
    TestRequest request = actionTester.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void execute_whenUserIsNotSystemAdministrator_shouldFailWithForbiddenException() {
    userSession.logIn();

    TestRequest request = actionTester.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void execute_whenNoLogNameParamProvided_shouldReturnAppLogs() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{app}");
  }

  @Test
  public void execute_whenFileDoesNotExist_shouldReturn404NotFound() throws IOException {
    logInAsSystemAdministrator();

    createLogsDir();

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void execute_whenLogNameProvided_shouldRespondWithLogsAccording() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    asList("ce", "es", "web", "access", "deprecation").forEach(process -> {
      TestResponse response = actionTester.newRequest()
        .setParam("name", process)
        .execute();
      assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
      assertThat(response.getInput()).isEqualTo("{" + process + "}");
    });
  }

  @Test
  public void execute_whenDataCenterEdition_shouldReturnZipFile() throws IOException {
    when(configuration.get(ProcessProperties.Property.WEB_PORT.getKey())).thenReturn(Optional.of("9000"));
    when(configuration.get(ProcessProperties.Property.WEB_PORT.getKey())).thenReturn(Optional.of("9001"));
    when(configuration.get(ProcessProperties.Property.WEB_PORT.getKey())).thenReturn(Optional.of("9002"));
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTesterDataCenter.newRequest()
      .setParam("name", "app")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.ZIP);
    assertZipContainsFiles(3, response, "sonar");
  }

  @Test
  public void execute_whenDataCenterEdition_andOnlyOneNode_shouldReturnZipFileWithOneFile() throws IOException {
    when(configuration.get(ProcessProperties.Property.WEB_PORT.getKey())).thenReturn(Optional.of("9000"));
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTesterDataCenter.newRequest()
      .setParam("name", "ce")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.ZIP);
    assertZipContainsFiles(1, response, "ce");
  }

  private void assertZipContainsFiles(int expectedFiles, TestResponse response, String expectedLog) throws IOException {
    InputStream inputStream = response.getInputStream();
    List<File> files = extractFilesFromZip(inputStream);

    files.forEach(f -> assertThat(f.getName()).contains(expectedLog));
    assertThat(files).hasSize(expectedFiles);
  }

  @Test
  public void execute_whenNumberRollingPolicy_shouldReturnLatestOnly() throws IOException {
    logInAsSystemAdministrator();

    File dir = createLogsDir();
    writeTestLogFile(dir, "sonar.1.log", "{old}");
    writeTestLogFile(dir, "sonar.log", "{recent}");

    TestResponse response = actionTester.newRequest()
      .setParam("name", "app")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{recent}");
  }

  @Test
  public void execute_whenDateRollingPolicy_shouldReturnLatestLogFile() throws IOException {
    logInAsSystemAdministrator();

    File dir = createLogsDir();
    writeTestLogFile(dir, "sonar.20210101.log", "{old}");
    writeTestLogFile(dir, "sonar.20210201.log", "{recent}");

    TestResponse response = actionTester.newRequest()
      .setParam("name", "app")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{recent}");
  }

  private void createAllLogsFiles() throws IOException {
    File dir = createLogsDir();
    writeTestLogFile(dir, "access.log", "{access}");
    writeTestLogFile(dir, "sonar.log", "{app}");
    writeTestLogFile(dir, "ce.log", "{ce}");
    writeTestLogFile(dir, "es.log", "{es}");
    writeTestLogFile(dir, "web.log", "{web}");
    writeTestLogFile(dir, "deprecation.log", "{deprecation}");

    writeTestLogFile(dir, "fake.access.log", "{fake-access}");
    writeTestLogFile(dir, "access.19900110.log", "{fake-access}");
    writeTestLogFile(dir, "fake.sonar.log", "{fake-app}");
    writeTestLogFile(dir, "sonar.19900110.log", "{date-app}");
    writeTestLogFile(dir, "fake.ce.log", "{fake-ce}");
    writeTestLogFile(dir, "ce.19900110.log", "{date-ce}");
    writeTestLogFile(dir, "fake.es.log", "{fake-es}");
    writeTestLogFile(dir, "es.19900110.log", "{date-es}");
    writeTestLogFile(dir, "fake.web.log", "{fake-web}");
    writeTestLogFile(dir, "web.19900110.log", "{date-web}");
    writeTestLogFile(dir, "fake.deprecation.log", "{fake-deprecation}");
    writeTestLogFile(dir, "deprecation.19900110.log", "{date-deprecation}");
  }

  private static void writeTestLogFile(File dir, String child, String data) throws IOException {
    FileUtils.write(new File(dir, child), data, Charset.defaultCharset());
  }

  private File createLogsDir() throws IOException {
    File dir = temp.newFolder();
    when(configuration.get(PATH_LOGS.getKey())).thenReturn(Optional.of(dir.getAbsolutePath()));
    return dir;
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }


  //helper methods for dealing with zip
  public static List<File> extractFilesFromZip(InputStream zipInputStream) throws IOException {
    List<File> extractedFiles = new ArrayList<>();
    File tempDir = Files.createTempDirectory("extractedZip").toFile();

    try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
      ZipEntry zipEntry;
      while ((zipEntry = zis.getNextEntry()) != null) {
        File newFile = newFile(tempDir, zipEntry);
        if (zipEntry.isDirectory()) {
          if (!newFile.isDirectory() && !newFile.mkdirs()) {
            throw new IOException("Failed to create directory " + newFile);
          }
        } else {
          // fix for Windows-created archives
          File parent = newFile.getParentFile();
          if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent);
          }

          // write file content
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
        extractedFiles.add(newFile);
      }
    }
    return extractedFiles;
  }

  private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    File destFile = new File(destinationDir, zipEntry.getName());

    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }

    return destFile;
  }
}
