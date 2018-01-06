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
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@Properties({
  @Property(
    key = TempFolderExtension.CREATE_TEMP_FILES,
    type = PropertyType.BOOLEAN,
    name = "Property to decide if it should create temp files",
    defaultValue = "false")
})
public class TempFolderExtension implements ServerExtension {

  private static final Logger LOG = Loggers.get(TempFolderExtension.class);

  public static final String CREATE_TEMP_FILES = "sonar.createTempFiles";

  private Settings settings;

  private TempFolder tempFolder;

  public TempFolderExtension(Settings settings, TempFolder tempFolder) {
    this.settings = settings;
    this.tempFolder = tempFolder;
    start();
  }

  public void start() {
    if (settings.getBoolean(CREATE_TEMP_FILES)) {
      LOG.info("Creating temp directory: " + tempFolder.newDir("sonar-it").getAbsolutePath());
      LOG.info("Creating temp file: " + tempFolder.newFile("sonar-it", ".txt").getAbsolutePath());
    }
  }

}
