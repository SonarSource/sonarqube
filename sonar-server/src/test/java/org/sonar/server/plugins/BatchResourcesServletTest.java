package org.sonar.server.plugins;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class BatchResourcesServletTest {
  private BatchResourcesServlet servlet;
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    servlet = new BatchResourcesServlet();
    request = mock(HttpServletRequest.class);
  }

  @Test
  public void shouldDetermineResource() {
    when(request.getContextPath()).thenReturn("sonar");
    when(request.getServletPath()).thenReturn("/batch");

    when(request.getRequestURI()).thenReturn("/sonar/batch/sonar-core-2.6.jar");
    assertThat(servlet.getResource(request), is("sonar-core-2.6.jar"));

    when(request.getRequestURI()).thenReturn("/sonar/batch/");
    assertThat(servlet.getResource(request), is(""));

    when(request.getRequestURI()).thenReturn("/sonar/batch");
    assertThat(servlet.getResource(request), is(""));
  }

  @Test
  public void shouldDetermineListOfResources() {
    ServletContext servletContext = mock(ServletContext.class);
    servlet = spy(servlet);
    doReturn(servletContext).when(servlet).getServletContext();
    Set<String> libs = Sets.newHashSet();
    libs.add("/WEB-INF/lib/sonar-core-2.6.jar");
    libs.add("/WEB-INF/lib/treemap.rb");
    libs.add("/WEB-INF/lib/directory/");
    when(servletContext.getResourcePaths(anyString())).thenReturn(libs);

    assertThat(servlet.getLibs().size(), is(1));
    assertThat(servlet.getLibs().get(0), is("sonar-core-2.6.jar"));
  }
}
