package org.sonar.server.ws;

import org.junit.Test;
import org.sonar.server.plugins.MimeTypes;
import org.sonarqube.ws.Issues;

import static org.assertj.core.api.Assertions.assertThat;

public class WsUtilsTest {

  @Test
  public void write_json_by_default() throws Exception {
    TestRequest request = new TestRequest();
    DumbResponse response = new DumbResponse();

    Issues.Issue msg = Issues.Issue.newBuilder().setKey("I1").build();
    WsUtils.writeProtobuf(msg, request, response);

    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.JSON);
    assertThat(response.outputAsString())
      .startsWith("{")
      .contains("\"key\":\"I1\"")
      .endsWith("}");
  }

  @Test
  public void write_protobuf() throws Exception {
    TestRequest request = new TestRequest();
    request.setMediaType(MimeTypes.PROTOBUF);
    DumbResponse response = new DumbResponse();

    Issues.Issue msg = Issues.Issue.newBuilder().setKey("I1").build();
    WsUtils.writeProtobuf(msg, request, response);

    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.PROTOBUF);
    assertThat(Issues.Issue.parseFrom(response.getFlushedOutput()).getKey()).isEqualTo("I1");
  }
}
