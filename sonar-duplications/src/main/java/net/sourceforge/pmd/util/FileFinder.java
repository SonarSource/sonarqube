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
