package org.sonar.runner;

public class SonarProcess extends Thread {

  final String name;
  final int port;

  public SonarProcess(String name, int port) {
    this.name = name;
    this.port = port;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      System.out.println("Thread[" + name + "] - ping");
      try {
        if (getName().equalsIgnoreCase("ES")) {
          Thread.sleep(20000);
        }
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        break;
      }
    }
    System.out.println("Shutting down Thread[" + name + "]");
  }
}