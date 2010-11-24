package org.sonar.core.classloaders;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

public class ClassLoadersCollectionTest {

  @Test
  public void shouldImport() throws Exception {
    String className = getClass().getName().replace(".", "/");
    ClassLoadersCollection collection = new ClassLoadersCollection(null);
    collection.createClassLoader("foo", Arrays.asList(getClass().getResource("/" + className + "/foo.jar")), false);
    collection.createClassLoader("bar", Arrays.asList(getClass().getResource("/" + className + "/bar.jar")), false);
    collection.done();

    String resourceName = "org/sonar/plugins/bar/api/resource.txt";
    assertThat(collection.get("bar").getResourceAsStream(resourceName), notNullValue());
    assertThat(collection.get("foo").getResourceAsStream(resourceName), notNullValue());
  }

}
