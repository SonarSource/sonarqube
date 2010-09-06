package tags;

import java.util.Collection;

public class File extends Content {

  public TagException getTagException() {
    return new TagException();
  }

  public void read() throws TagException {

  }
  
  public Collection<String> read(boolean flag) throws TagException {
    return null;
  }

  public String[] read(String name) throws TagException {
    return null;
  }

  public String read(Collection<File> files, String name) throws RuntimeException {
    return "";
  }
}
