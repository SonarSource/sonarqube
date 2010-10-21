package org.sonar.core.classloaders;

import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class ResourcesClassLoaderTest {

  @Test
  public void test() throws Exception {
    List<URL> urls = Arrays.asList(new URL("http://localhost:9000/deploy/plugins/checkstyle/extension.xml"));
    ResourcesClassLoader classLoader = new ResourcesClassLoader(urls, null);
    assertThat(classLoader.findResource("extension.xml"), notNullValue());
  }
}
