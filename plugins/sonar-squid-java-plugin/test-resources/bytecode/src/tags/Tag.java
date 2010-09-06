package tags;

import java.util.Collection;

public interface Tag extends Comment {
	
	public TagName getName();
	
	public Collection<Line> getLines();

}
