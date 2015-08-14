package sample;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class SampleTest {

  @Test
  public void should_return_i() {
    Sample sample = new Sample(1);
    assertThat(sample.getI(), CoreMatchers.is(1));
  }

  @Test
  public void should_return_to_string() {
    assertThat(new Sample(1).toString(), CoreMatchers.is("1"));
  }

}
