package util;/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class ItUtils {

  private ItUtils() {
  }

  private static final Supplier<File> HOME_DIR = Suppliers.memoize(new Supplier<File>() {
    @Override
    public File get() {
      File dir = new File("it");

      // intellij way
      if (dir.exists() && dir.isDirectory()) {
        return dir.getParentFile();
      }

      // maven way
      dir = new File("../it");
      if (dir.exists() && dir.isDirectory()) {
        return dir.getParentFile();
      }

      throw new IllegalStateException("Fail to locate home directory");
    }
  });

  public static FileLocation xooPlugin() {
    File target = new File(HOME_DIR.get(), "plugins/sonar-xoo-plugin/target");
    if (target.exists()) {
      for (File jar : FileUtils.listFiles(target, new String[] {"jar"}, false)) {
        if (jar.getName().startsWith("sonar-xoo-plugin-") && !jar.getName().contains("-source")) {
          return FileLocation.of(jar);
        }
      }
    }
    throw new IllegalStateException("XOO plugin is not built");
  }

  /**
   * Locate the directory of sample project
   *
   * @param relativePath path related to the directory it/projects, for example "qualitygate/xoo-sample"
   */
  public static File projectDir(String relativePath) {
    File dir = new File(HOME_DIR.get(), "it/projects/" + relativePath);
    if (!dir.exists() || !dir.isDirectory()) {
      throw new IllegalStateException("Directory does not exist: " + dir.getAbsolutePath());
    }
    return dir;
  }
}
