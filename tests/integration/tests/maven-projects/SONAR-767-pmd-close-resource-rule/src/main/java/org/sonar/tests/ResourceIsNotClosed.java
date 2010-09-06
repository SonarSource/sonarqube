package org.sonar.tests;

public class ResourceIsNotClosed {

  public void resourceIsNotStopped() throws Exception {
    CloseableResource resource = new CloseableResource();
    try {
      resource.init();

    } finally {
      //resource.stop();
    }
  }
}
