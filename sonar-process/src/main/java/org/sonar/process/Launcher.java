package org.sonar.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @Since 4.5
 */
public class Launcher extends Thread {

  private final static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

  final String name;
  final DatagramSocket socket;
  java.lang.Process process;


  public Launcher(String name, DatagramSocket socket) {
    LOGGER.info("Creating Launcher for '{}' with base port: {}", name, socket.getLocalPort());
    this.name = name;
    this.socket = socket;
  }

  private void launch() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Runner.main(name, socket.getLocalPort() + "");
      }
    }).start();
  }

  private void shutdown() {
    process.destroy();
  }

  private void monitor() {
    long ping = Long.MAX_VALUE;
    try {
      while (true) {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        long newPing = System.currentTimeMillis();
        String message = new String(packet.getData(), 0, 0, packet.getLength());
        LOGGER.info("{} last seen since {}ms", message, (newPing - ping));
        if ((newPing - ping) > 3000) {
          // close everything here...
        }
        ping = newPing;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Launcher Thread for " + name + " could not communicate to socket", e);
    }
  }

  @Override
  public void run() {
    LOGGER.info("launching child VM for " + name);
    launch();

    LOGGER.info("Monitoring VM for " + name);
    while (true) {
      monitor();
    }
  }
}
