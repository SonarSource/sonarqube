/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process.systeminfo;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

public class SystemInfoHttpServerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SystemInfoSectionProvider stateProvider1 = new ProcessStateProvider("state1");
  SystemInfoSectionProvider stateProvider2 = new ProcessStateProvider("state2");

  @Test
  public void start_and_stop() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(PROPERTY_PROCESS_INDEX, "1");
    properties.setProperty(PROPERTY_SHARED_PATH, temp.newFolder().getAbsolutePath());
    SystemInfoHttpServer underTest = new SystemInfoHttpServer(properties, Arrays.asList(stateProvider1, stateProvider2));

    underTest.start();
    Response response = call(underTest.getUrl());
    assertThat(response.code()).isEqualTo(200);
    ProtobufSystemInfo.SystemInfo systemInfo = ProtobufSystemInfo.SystemInfo.parseFrom(response.body().bytes());
    assertThat(systemInfo.getSectionsCount()).isEqualTo(2);
    assertThat(systemInfo.getSections(0).getName()).isEqualTo("state1");
    assertThat(systemInfo.getSections(1).getName()).isEqualTo("state2");

    underTest.stop();
    expectedException.expect(ConnectException.class);
    call(underTest.getUrl());
  }

  private static Response call(String url) throws IOException {
    Request request = new Request.Builder().get().url(url).build();
    return new OkHttpClient().newCall(request).execute();
  }
}
