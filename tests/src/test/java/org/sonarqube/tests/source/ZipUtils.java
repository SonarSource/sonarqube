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
package org.sonarqube.tests.source;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

  // I see that we have apache commons compress in the path so it should be possible to reduce the boiler plate
  // -- but nothing out of the box : still have to walk the tree and uncompress file by file.

  static void unzip(File archive, String directory) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {

      ZipEntry entry = zis.getNextEntry();
      while (entry != null) {

        File file = new File(directory, entry.getName());

        if (entry.isDirectory()) {
          file.mkdirs();
        } else {

          File parent = file.getParentFile();
          if (!parent.exists()) {
            parent.mkdirs();
          }

          try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            long size = entry.getSize();
            byte[] buffer = new byte[Math.toIntExact(size)];
            if (size > 0) {
              int location;
              while ((location = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, location);
              }
            }
          }

        }

        entry = zis.getNextEntry();
      }
    }
  }

}
