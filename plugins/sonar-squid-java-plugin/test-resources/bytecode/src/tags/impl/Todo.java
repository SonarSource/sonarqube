package tags.impl;

import java.util.ArrayList;
import java.util.Collection;

import tags.File;
import tags.Language;
import tags.Line;
import tags.SourceFile;
import tags.Tag;
import tags.TagException;
import tags.TagName;

public class Todo<E extends Language> implements Tag {

	private File file = new SourceFile();

	private ArrayList<Line> lines = new ArrayList<Line>();

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public void addLine(Line line) {
		lines.add(line);
	}

	public boolean useSourceFile(SourceFile file) throws TagException {
		return false;
	}

	public TagName getName() {
		return new TagName("TODO");
	}

	public Collection<Line> getLines() {
		return lines;
	}

}
