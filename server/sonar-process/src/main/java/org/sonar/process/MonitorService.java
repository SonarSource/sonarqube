package org.sonar.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class MonitorService extends Thread {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

  final DatagramSocket socket;
  final Map<String, Thread> processes;
  final Map<String, Long> processesPing;

  public MonitorService(DatagramSocket socket) {
    this.socket = socket;
    processes = new HashMap<String, Thread>();
    processesPing = new HashMap<String, Long>();
  }

  public void register(ProcessWrapper process) {
    this.processes.put(process.getName(), process);
    this.processesPing.put(process.getName(), System.currentTimeMillis());
  }

  @Override
  public void run() {
    LOGGER.info("Launching Monitoring Thread");
    long time = System.currentTimeMillis();
    while (!currentThread().isInterrupted()) {
      DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
      try {
        socket.setSoTimeout(200);
        socket.receive(packet);
        time = System.currentTimeMillis();
        String message = new String(packet.getData());
        long lastTime = processesPing.get(message);
        processesPing.put(message, time);
        LOGGER.info("{} last seen since {}ms", message, (time - lastTime));
      } catch (Exception e) {
        // Do nothing
      }
      if (!checkAllProcessPing(time)) {
        break;
      }
    }
    LOGGER.info("Some app has not been pinging");
    for (Thread process : processes.values()) {
      if (!process.isInterrupted()) {
        process.interrupt();
      }
    }
  }


  private boolean checkAllProcessPing(long now) {

    //check that all thread wrapper are running
    for (Thread thread : processes.values()) {
      if (thread.isInterrupted() || !thread.isAlive()) {
        return false;
      }
    }

    //check that all heartbeats are OK
    for (Long ping : processesPing.values()) {
      if ((now - ping) > 3000) {
        return false;
      }
    }
    return true;
  }

  public Integer getMonitoringPort() {
    return socket.getPort();
  }
}
