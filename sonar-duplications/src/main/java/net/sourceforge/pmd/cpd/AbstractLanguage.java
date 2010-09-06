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
