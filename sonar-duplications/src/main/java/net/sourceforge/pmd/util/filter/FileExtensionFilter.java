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
