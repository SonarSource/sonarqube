package org.sonar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.MonitoredProcess;
import org.sonar.process.Props;

import java.util.Properties;

public class DummyOKApplication extends MonitoredProcess {

  private static final Logger LOGGER = LoggerFactory.getLogger(DummyOKApplication.class);

  protected DummyOKApplication(Props props) throws Exception {
    super(props);
  }

  @Override
  protected void doStart() {
    LOGGER.info("Starting Dummy OK Process");
  }

  @Override
  protected void doTerminate() {
    LOGGER.info("Terminating Dummy OK Process");
  }

  @Override
  protected boolean doIsReady() {
    return false;
  }

  public static void main(String... args) throws Exception {
    Props props = new Props(new Properties());
    props.set(MonitoredProcess.NAME_PROPERTY, DummyOKApplication.class.getSimpleName());
    new DummyOKApplication(props).start();
    System.exit(1);
  }
}
