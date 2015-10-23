package it.plugins;

import com.google.common.base.Function;
import java.io.File;
import java.util.Collection;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import util.ItUtils;

import static com.google.common.collect.FluentIterable.from;

public class Project {

  public static File basedir() {
    return ItUtils.projectDir("plugins/project");
  }

  public static Iterable<String> allFilesInDir(final String dirPath) {
    Collection<File> files = FileUtils.listFiles(new File(basedir(), dirPath), null, true);
    return from(files).transform(new Function<File, String>() {
      @Nullable
      public String apply(File file) {
        // transforms /absolute/path/to/src/java/Foo.java to src/java/Foo.java
        String filePath = FilenameUtils.separatorsToUnix(file.getPath());
        return dirPath + StringUtils.substringAfterLast(filePath, dirPath);
      }
    });
  }
}
