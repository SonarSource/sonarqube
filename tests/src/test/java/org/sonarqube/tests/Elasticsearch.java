package org.sonarqube.tests;

import java.net.InetAddress;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper to directly access Elasticsearch. It requires the HTTP port
 * to be open.
 */
public class Elasticsearch {

  private final int httpPort;

  Elasticsearch(int httpPort) {
    this.httpPort = httpPort;
  }

  /**
   * Forbid indexing requests on the specified index. Index becomes read-only.
   */
  public void lockWrites(String index) throws Exception {
    putIndexSetting(httpPort, index, "blocks.write", "true");
  }

  /**
   * Enable indexing requests on the specified index.
   * @see #lockWrites(String)
   */
  public void unlockWrites(String index) throws Exception {
    putIndexSetting(httpPort, index, "blocks.write", "false");
  }

  private void putIndexSetting(int searchHttpPort, String index, String key, String value) throws Exception {
    Request.Builder request = new Request.Builder()
      .url("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + searchHttpPort + "/" + index + "/_settings")
      .put(RequestBody.create(MediaType.parse("application/json"), "{" +
        "    \"index\" : {" +
        "        \"" + key + "\" : \"" + value + "\"" +
        "    }" +
        "}"));
    OkHttpClient okClient = new OkHttpClient.Builder().build();
    Response response = okClient.newCall(request.build()).execute();
    assertThat(response.isSuccessful()).isTrue();
  }
}
