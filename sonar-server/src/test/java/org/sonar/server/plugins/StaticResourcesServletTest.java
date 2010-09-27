package org.sonar.server.plugins;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticResourcesServletTest {

  private StaticResourcesServlet servlet;
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    servlet = new StaticResourcesServlet();
    request = mock(HttpServletRequest.class);
  }

  @Test
  public void shouldDeterminePluginKey() {
    when(request.getContextPath()).thenReturn("/");
    when(request.getServletPath()).thenReturn("static");
    when(request.getRequestURI()).thenReturn("/static/myplugin/image.png");
    assertThat(servlet.getPluginKey(request), is("myplugin"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/images/image.png");
    assertThat(servlet.getPluginKey(request), is("myplugin"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/");
    assertThat(servlet.getPluginKey(request), is("myplugin"));
  }

  @Test
  public void shouldDetermineResourcePath() {
    when(request.getContextPath()).thenReturn("/");
    when(request.getServletPath()).thenReturn("static");
    when(request.getRequestURI()).thenReturn("/static/myplugin/image.png");
    assertThat(servlet.getResourcePath(request), is("/static/image.png"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/images/image.png");
    assertThat(servlet.getResourcePath(request), is("/static/images/image.png"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/");
    assertThat(servlet.getResourcePath(request), is("/static/"));
  }
}
