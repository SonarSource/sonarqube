package org.sonar.api.scan.filesystem;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.Serializable;
import java.util.Map;

public interface InputFile extends Serializable {

  /**
   * Canonical path of source directory.
   * Example: <code>/path/to/module/src/main/java</code> or <code>C:\path\to\module\src\main\java</code>
   */
  String ATTRIBUTE_SOURCEDIR_PATH = "srcDirPath";

  /**
   * Relative path from source directory. File separator is the forward slash ('/'),
   * even on MSWindows.
   */
  String ATTRIBUTE_SOURCE_RELATIVE_PATH = "srcRelPath";

  /**
   * Detected language
   */
  String ATTRIBUTE_LANGUAGE = "lang";

  /**
   *
   */
  String ATTRIBUTE_TYPE = "type";
  String TYPE_SOURCE = "source";
  String TYPE_TEST = "test";

  String ATTRIBUTE_STATUS = "status";
  String STATUS_SAME = "same";
  String STATUS_CHANGED = "changed";
  String STATUS_ADDED = "added";

  String ATTRIBUTE_HASH = "hash";
  String ATTRIBUTE_EXTENSION = "extension";


  /**
   * Path from module base directory. Path is unique and identifies file within given
   * <code>{@link org.sonar.api.scan.filesystem.ModuleFileSystem}</code>. File separator is the forward slash ('/'),
   * even on MSWindows.
   * <p/>
   * If:
   * <ul>
   * <li>Module base dir is <code>/absolute/path/to/module</code></li>
   * <li>File is <code>/absolute/path/to/module/src/main/java/com/Foo.java</code></li>
   * </ul>
   * then the path is <code>src/main/java/com/Foo.java</code>
   * <p/>
   * On MSWindows, if:
   * <ul>
   * <li>Module base dir is <code>C:\absolute\path\to\module</code></li>
   * <li>File is <code>C:\absolute\path\to\module\src\main\java\com\Foo.java</code></li>
   * </ul>
   * then the path is <code>src/main/java/com/Foo.java</code>.
   * <p/>
   * Returned relative path is never null.
   */
  String relativePath();

  /**
   * Canonical path.
   */
  String path();

  /**
   * Not-null related {@link java.io.File}
   */
  File file();

  /**
   * Not-null filename, including extension
   */

  String name();

  /**
   * Does the given attribute have the given value ?
   */
  boolean has(String attribute, String value);

  /**
   * See list of attribute keys in constants starting with ATTRIBUTE_.
   */
  @CheckForNull
  String attribute(String key);

  Map<String, String> attributes();
}
