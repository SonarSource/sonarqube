package tags.impl;

import java.util.Collection;

import tags.File;
import tags.Line;
import tags.SourceFile;
import tags.Tag;
import tags.TagName;

public class FixMe implements Tag {

	public FixMe() {
		System.out.println(SourceFile.path);
		File file = new File();
		file.getTagException().getMessage();
	}

	public Collection<Line> getLines() {
	  Comparable comparator = new Comparable<FixMe>() {
	    public int compareTo(FixMe fixMe){
	      return 0;
	    }
    };
		return null;
	}

	public TagName getName() {
		return new TagName("FIXME");
	}
	

  public int coucouc(String s, Integer integer) {
    return 0;
  }

}
