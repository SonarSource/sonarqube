package org.sonar.batch.protocol.input;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;

public class QProfileTest {

  @Test
  public void testEqualsAndHashCode() throws ParseException {
    QProfile qProfile1 = new QProfile("squid-java", "Java", "java", new SimpleDateFormat("dd/MM/yyyy").parse("14/03/1984"));
    QProfile qProfile2 = new QProfile("squid-java", "Java 2", "java", new SimpleDateFormat("dd/MM/yyyy").parse("14/03/1985"));

    assertThat(qProfile1.equals(qProfile1)).isTrue();
    assertThat(qProfile1.equals("foo")).isFalse();
    assertThat(qProfile1.equals(qProfile2)).isTrue();

    assertThat(qProfile1.hashCode()).isEqualTo(148572637);
  }
}
