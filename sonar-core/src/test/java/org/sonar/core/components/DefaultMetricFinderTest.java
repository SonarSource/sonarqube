package org.sonar.core.components;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DefaultMetricFinderTest extends AbstractDbUnitTestCase {

  private DefaultMetricFinder finder;

  @Before
  public void setUp() {
    setupData("shared");
    finder = new DefaultMetricFinder(getSessionFactory());
  }

  @Test
  public void shouldFindAll() {
    assertThat(finder.findAll().size(), is(2));
  }

  @Test
  public void shouldFindByKeys() {
    assertThat(finder.findAll(Arrays.<String> asList("ncloc", "foo", "coverage")).size(), is(2));
  }

  @Test
  public void shouldFindById() {
    assertThat(finder.findById(1).getKey(), is("ncloc"));
    assertThat(finder.findById(3), nullValue());
  }

  @Test
  public void shouldFindByKey() {
    assertThat(finder.findByKey("ncloc").getKey(), is("ncloc"));
    assertThat(finder.findByKey("disabled"), nullValue());
  }
}
