package org.sonar.tests;

public class ResourceIsClosed {
  
  public void resourceIsStopped() throws Exception {
    CloseableResource resource = new CloseableResource();
    try {
      resource.init();

    } finally {
      resource.close();
    }
  }
}
