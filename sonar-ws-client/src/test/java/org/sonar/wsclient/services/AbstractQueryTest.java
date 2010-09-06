package org.sonar.wsclient.services;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.JdkUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;

public class AbstractQueryTest {

  @BeforeClass
  public static void before() {
    WSUtils.setInstance(new JdkUtils());
  }

  @AfterClass
  public static void after() {
    WSUtils.setInstance(null);
  }

  @Test
  public void appendUrlParameter() {
    StringBuilder url = new StringBuilder();
    AbstractQuery.appendUrlParameter(url, "foo", "bar");
    assertEquals("foo=bar&", url.toString());
    AbstractQuery.appendUrlParameter(url, "foo2", "bar2");
    assertEquals("foo=bar&foo2=bar2&", url.toString());
  }

  @Test
  public void appendUrlBooleanParameter() {
    StringBuilder url = new StringBuilder();
    AbstractQuery.appendUrlParameter(url, "foo", Boolean.TRUE);
    assertEquals("foo=true&", url.toString());
  }

  @Test
  public void appendUrlIntParameter() {
    StringBuilder url = new StringBuilder();
    AbstractQuery.appendUrlParameter(url, "foo", 9);
    assertEquals("foo=9&", url.toString());
  }

  @Test
  public void appendUrlArrayParameter() {
    StringBuilder url = new StringBuilder();
    AbstractQuery.appendUrlParameter(url, "foo", new String[]{"bar", "bar2"});
    assertEquals("foo=bar,bar2&", url.toString());
  }

  @Test
  public void appendUrlNullParameter() {
    StringBuilder url = new StringBuilder();
    AbstractQuery.appendUrlParameter(url, "foo", null);
    assertEquals("", url.toString());
  }

  @Test
  public void appendUrlDateParameter() throws ParseException {
    StringBuilder url = new StringBuilder();
    Date date = new SimpleDateFormat("dd/MM/yyyy").parse("25/12/2009");
    AbstractQuery.appendUrlParameter(url, "date", date, false);
    assertEquals("date=2009-12-25&", url.toString());
  }

  @Test
  public void appendUrlDateTimeParameter() throws ParseException {
    TimeZone defaultTimeZone = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("PST"));
      StringBuilder url = new StringBuilder();
      Date date = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse("25/12/2009 15:59");
      AbstractQuery.appendUrlParameter(url, "date", date, true);
      assertEquals("date=2009-12-25T15%3A59%3A00-0800&", url.toString());

    } finally {
      TimeZone.setDefault(defaultTimeZone);
    }
  }
}
