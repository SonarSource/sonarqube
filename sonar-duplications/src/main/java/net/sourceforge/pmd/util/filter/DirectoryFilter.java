package net.sourceforge.pmd.util.filter;

import java.io.File;

/**
 * Directory filter.
 */
public class DirectoryFilter implements Filter<File> {
	public static final DirectoryFilter INSTANCE = new DirectoryFilter();

	private DirectoryFilter() {
	}

	public boolean filter(File file) {
		return file.isDirectory();
	}
}
