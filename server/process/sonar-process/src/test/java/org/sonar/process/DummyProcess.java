/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class DummyProcess extends MonitoredProcess {

  public static final String NAME = "DummyName";
  public static final String CHECKFILE_NAME = "check.tmp";

  private static final Logger LOGGER = LoggerFactory.getLogger(DummyProcess.class);

  private boolean isReady = false;
  private boolean isRunning = true;
  private File checkFile;


  protected DummyProcess(Props props, boolean monitored) throws Exception {
    super(props, monitored);
  }

  protected DummyProcess(Props props) throws Exception {
    super(props);
  }

  @Override
  protected void doStart() {
    isReady = true;
    checkFile = new File(FileUtils.getTempDirectory(), CHECKFILE_NAME);
    LOGGER.info("Starting Dummy OK Process");
    while (isRunning) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        isRunning = false;
      }
    }
  }

  @Override
  protected void doTerminate() {
    LOGGER.info("Terminating Dummy OK Process");
    this.isRunning = false;
  }

  @Override
  protected boolean doIsReady() {
    return isReady;
  }

  public static void main(String[] args) throws Exception {
    Props props = new Props(new Properties());
    props.set(MonitoredProcess.NAME_PROPERTY, DummyProcess.class.getSimpleName());
    new DummyProcess(props).start();
    System.exit(1);
  }

  public File getCheckFile() {
    return checkFile;
  }
}
