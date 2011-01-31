/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package net.sourceforge.pmd.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for finding files within a directory.
 */
public class FileFinder {

    private FilenameFilter filter;
    private static final String FILE_SEP = System.getProperty("file.separator");

    public List<File> findFilesFrom(String dir, FilenameFilter filter, boolean recurse) {
        this.filter = filter;
        List<File> files = new ArrayList<File>();
        scanDirectory(new File(dir), files, recurse);
        return files;
    }

    /**
     * Implements a tail recursive file scanner
     */
    private void scanDirectory(File dir, List<File> list, boolean recurse) {
        String[] candidates = dir.list(filter);
        if (candidates == null) {
            return;
        }
        for (int i = 0; i < candidates.length; i++) {
            File tmp = new File(dir + FILE_SEP + candidates[i]);
            if (tmp.isDirectory()) {
                if (recurse) {
                    scanDirectory(tmp, list, true);
                }
            } else {
                list.add(new File(dir + FILE_SEP + candidates[i]));
            }
        }
    }
}
