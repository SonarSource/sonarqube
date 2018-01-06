/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.storage;

import com.persistit.Persistit;
import com.persistit.exception.PersistitException;
import com.persistit.logging.Slf4jAdapter;
import java.io.File;
import java.util.Properties;
import org.picocontainer.Startable;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.utils.TempFolder;

import static org.sonar.core.util.FileUtils.deleteQuietly;

/**
 * Factory of storages
 *
 * @since 3.6
 */
@ScannerSide
public class StoragesManager implements Startable {
  private File tempDir;
  private Persistit persistit;
  private final TempFolder tempFolder;

  public StoragesManager(TempFolder tempFolder) {
    this.tempFolder = tempFolder;
    initPersistit();
  }

  private void initPersistit() {
    try {
      tempDir = tempFolder.newDir("caches");
      persistit = new Persistit();
      persistit.setPersistitLogger(new Slf4jAdapter(LoggerFactory.getLogger("PERSISTIT")));
      Properties props = new Properties();
      props.setProperty("datapath", tempDir.getAbsolutePath());
      props.setProperty("logpath", "${datapath}/log");
      props.setProperty("logfile", "${logpath}/persistit_${timestamp}.log");
      props.setProperty("buffer.count.8192", "10");
      props.setProperty("journalpath", "${datapath}/journal");
      props.setProperty("tmpvoldir", "${datapath}");
      props.setProperty("volume.1", "${datapath}/persistit,create,pageSize:8192,initialPages:10,extensionPages:100,maximumPages:25000");
      props.setProperty("jmx", "false");
      persistit.setProperties(props);
      persistit.initialize();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to start caches", e);
    }
  }

  @Override
  public void start() {
    // already started in constructor
  }

  @Override
  public void stop() {
    if (persistit != null) {
      try {
        persistit.close(false);
        persistit = null;
      } catch (PersistitException e) {
        throw new IllegalStateException("Fail to close caches", e);
      }
    }
    deleteQuietly(tempDir);
    tempDir = null;
  }

  File tempDir() {
    return tempDir;
  }

  Persistit persistit() {
    return persistit;
  }
}
