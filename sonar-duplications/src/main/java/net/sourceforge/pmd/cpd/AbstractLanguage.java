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

package net.sourceforge.pmd.cpd;

import net.sourceforge.pmd.util.filter.Filters;

import java.io.FilenameFilter;

public abstract class AbstractLanguage implements Language {
	private final Tokenizer tokenizer;
	private final FilenameFilter fileFilter;

	public AbstractLanguage(Tokenizer tokenizer, String... extensions) {
		this.tokenizer = tokenizer;
		fileFilter = net.sourceforge.pmd.util.filter.Filters.toFilenameFilter(Filters.getFileExtensionOrDirectoryFilter(extensions));
	}

	public FilenameFilter getFileFilter() {
		return fileFilter;
	}

	public Tokenizer getTokenizer() {
		return tokenizer;
	}
}
