/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.ws;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.server.computation.ReportQueue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IsQueueEmptyWebServiceTest {

  IsQueueEmptyWebService.IsQueueEmptyWsAction sut;
  ReportQueue queue;
  Response response;

  @Before
  public void before() throws Exception {
    queue = mock(ReportQueue.class);
    sut = new IsQueueEmptyWebService.IsQueueEmptyWsAction(queue);

    response = mock(Response.class);
    when(response.stream()).thenReturn(new FakeStream());
  }

  @Test
  public void send_true_when_queue_is_empty() throws Exception {
    when(queue.all()).thenReturn(new ArrayList<AnalysisReportDto>());

    sut.handle(mock(Request.class), response);

    assertThat(response.stream().toString()).isEqualTo("true");
  }

  @Test
  public void send_false_when_queue_is_not_empty() throws Exception {
    when(queue.all()).thenReturn(Lists.newArrayList(AnalysisReportDto.newForTests(1L)));

    sut.handle(mock(Request.class), response);

    assertThat(response.stream().toString()).isEqualTo("false");
  }

  private class FakeStream implements Response.Stream {
    private ByteArrayOutputStream stream;

    private FakeStream() {
      this.stream = new ByteArrayOutputStream();
    }

    public String toString() {
      return stream.toString();
    }

    @Override
    public Response.Stream setMediaType(String s) {
      return null;
    }

    @Override
    public Response.Stream setStatus(int httpStatus) {
      return null;
    }

    @Override
    public OutputStream output() {
      return stream;
    }
  }
}
