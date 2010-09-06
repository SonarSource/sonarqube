package net.sourceforge.pmd.util.filter;

import java.io.File;

public class FileExtensionFilter implements Filter<File> {
	protected final String[] extensions;
	protected final boolean ignoreCase;

	/**
	 * Matches any files with the given extensions, ignoring case
	 */
	public FileExtensionFilter(String... extensions) {
		this(true, extensions);
	}

	/**
	 * Matches any files with the given extensions, optionally ignoring case.
	 */
	public FileExtensionFilter(boolean ignoreCase, String... extensions) {
		this.extensions = extensions;
		this.ignoreCase = ignoreCase;
		if (ignoreCase) {
			for (int i = 0; i < this.extensions.length; i++) {
				this.extensions[i] = this.extensions[i].toUpperCase();
			}
		}
	}

	public boolean filter(File file) {
		boolean accept = extensions == null;
		if (!accept) {
			for (String extension : extensions) {
				String name = file.getName();
				if (ignoreCase ? name.toUpperCase().endsWith(extension) : name.endsWith(extension)) {
					accept = true;
					break;
				}
			}
		}
		return accept;
	}
}
