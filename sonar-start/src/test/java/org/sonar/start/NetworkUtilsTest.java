package org.sonar.start;

import org.junit.Test;

import java.net.ServerSocket;

import static org.fest.assertions.Assertions.assertThat;

public class NetworkUtilsTest {


  @Test
  public void find_free_port() throws Exception {
    int port = NetworkUtils.freePort();
    assertThat(port).isGreaterThan(1024);
  }

  @Test
  public void find_multiple_free_port() throws Exception {
    int port1 = NetworkUtils.freePort();
    int port2 = NetworkUtils.freePort();

    assertThat(port1).isGreaterThan(1024);
    assertThat(port2).isGreaterThan(1024);

    assertThat(port1).isNotSameAs(port2);
  }

  @Test
  public void find_multiple_free_non_adjacent_port() throws Exception {
    int port1 = NetworkUtils.freePort();

    ServerSocket socket = new ServerSocket(port1 + 1);

    int port2 = NetworkUtils.freePort();

    assertThat(port1).isGreaterThan(1024);
    assertThat(port2).isGreaterThan(1024);

    assertThat(port1).isNotSameAs(port2);
  }
}