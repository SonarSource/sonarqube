package org.sonar.core.classloaders;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

/**
 * This class loader is used to load resources from a list of URLs - see SONAR-1861.
 */
public class ResourcesClassLoader extends URLClassLoader {
  private Collection<URL> urls;

  public ResourcesClassLoader(Collection<URL> urls, ClassLoader parent) {
    super(new URL[] {}, parent);
    List<URL> filtered = Lists.newArrayList();
    for (URL url : urls) {
      String path = url.getPath();
      if ( !StringUtils.endsWith(path, ".jar") && !StringUtils.endsWith(path, "/")) {
        filtered.add(url);
      }
    }
    this.urls = filtered;
  }

  @Override
  public URL findResource(String name) {
    for (URL url : urls) {
      if (StringUtils.endsWith(url.getPath(), name)) {
        return url;
      }
    }
    return null;
  }
}
