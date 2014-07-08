package org.sonar.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @Since 4.5
 */
public abstract class Process implements Runnable {

  private final static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

  final String name;
  final int port;

  public Process(String name, int port) {
    this.name = name;
    this.port = port;
  }

  public abstract void execute();

  @Override
  public void run() {
    DatagramPacket client = null;
    try {
      byte[] data = new byte[name.length()];
      name.getBytes(0, name.length(), data, 0);
      DatagramPacket pack =
        new DatagramPacket(data, data.length, InetAddress.getLocalHost(), port);
      while (true) {
        DatagramSocket ds = new DatagramSocket();
        ds.send(pack);
        ds.close();
        Thread.sleep(1000);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Monitoring Thread for " + name + " could not communicate to socket", e);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Monitoring Thread for " + name + " is interrupted ", e);
    }
  }

  public static void main(final String... args) {

    Process process = new Process(args[0], Integer.parseInt(args[1])) {
      @Override
      public void execute() {
        while (true) {
          LOGGER.info("pseudo running for process {}", args[0]);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    Thread monitor = new Thread(process);
    monitor.start();
    process.execute();
    monitor.interrupt();
  }
}