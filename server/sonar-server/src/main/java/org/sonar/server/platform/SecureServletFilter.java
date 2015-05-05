/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.sonar.api.config.Settings;
import org.sonar.api.web.ServletFilter;

public class SecureServletFilter extends ServletFilter {
	  private String xssProtection;
	  private String frameOptions;
	  private String contentTypeOptions;

	  public SecureServletFilter(Settings settings) {
		  xssProtection = settings.getString("sonar.web.xssProtection");
		  frameOptions = settings.getString("sonar.web.frameOptions");
		  contentTypeOptions = settings.getString("sonar.web.contentTypeOptions");
	  }

	  @Override
	  public void init(FilterConfig config) throws ServletException {
	  }
	  
	  private class InnerResponse implements HttpServletResponse{
		private final HttpServletResponse inner;
		
		InnerResponse(HttpServletResponse x){
			this.inner = x;
		}

		@Override
		public void reset() {
		  inner.reset();
			
		  //Ruby resets the request, so we need to re-add the cheaders
		  if ( xssProtection != null && !inner.containsHeader("X-XSS-Protection") )
			  inner.setHeader("X-XSS-Protection", xssProtection);
		  if ( frameOptions != null && !inner.containsHeader("X-Frame-Options")  )
			  inner.setHeader("X-Frame-Options", frameOptions);
		  if ( contentTypeOptions != null && !inner.containsHeader("X-Content-Type-Options")  )
			  inner.setHeader("X-Content-Type-Options", contentTypeOptions);
		}
		
		public void addCookie(Cookie arg0) {
			inner.addCookie(arg0);
		}

		public void addDateHeader(String arg0, long arg1) {
			inner.addDateHeader(arg0, arg1);
		}

		public void addHeader(String arg0, String arg1) {
			inner.addHeader(arg0, arg1);
		}

		public void addIntHeader(String arg0, int arg1) {
			inner.addIntHeader(arg0, arg1);
		}

		public boolean containsHeader(String arg0) {
			return inner.containsHeader(arg0);
		}

		public String encodeRedirectURL(String arg0) {
			return inner.encodeRedirectURL(arg0);
		}

		public String encodeRedirectUrl(String arg0) {
			return inner.encodeRedirectUrl(arg0);
		}

		public String encodeURL(String arg0) {
			return inner.encodeURL(arg0);
		}

		public String encodeUrl(String arg0) {
			return inner.encodeUrl(arg0);
		}

		public void flushBuffer() throws IOException {
			inner.flushBuffer();
		}

		public int getBufferSize() {
			return inner.getBufferSize();
		}

		public String getCharacterEncoding() {
			return inner.getCharacterEncoding();
		}

		public String getContentType() {
			return inner.getContentType();
		}

		public String getHeader(String arg0) {
			return inner.getHeader(arg0);
		}

		public Collection<String> getHeaderNames() {
			return inner.getHeaderNames();
		}

		public Collection<String> getHeaders(String arg0) {
			return inner.getHeaders(arg0);
		}

		public Locale getLocale() {
			return inner.getLocale();
		}

		public ServletOutputStream getOutputStream() throws IOException {
			return inner.getOutputStream();
		}

		public int getStatus() {
			return inner.getStatus();
		}

		public PrintWriter getWriter() throws IOException {
			return inner.getWriter();
		}

		public boolean isCommitted() {
			return inner.isCommitted();
		}

		public void resetBuffer() {
			inner.resetBuffer();
		}

		public void sendError(int arg0, String arg1) throws IOException {
			inner.sendError(arg0, arg1);
		}

		public void sendError(int arg0) throws IOException {
			inner.sendError(arg0);
		}

		public void sendRedirect(String arg0) throws IOException {
			inner.sendRedirect(arg0);
		}

		public void setBufferSize(int arg0) {
			inner.setBufferSize(arg0);
		}

		public void setCharacterEncoding(String arg0) {
			inner.setCharacterEncoding(arg0);
		}

		public void setContentLength(int arg0) {
			inner.setContentLength(arg0);
		}

		public void setContentType(String arg0) {
			inner.setContentType(arg0);
		}

		public void setDateHeader(String arg0, long arg1) {
			inner.setDateHeader(arg0, arg1);
		}

		public void setHeader(String arg0, String arg1) {
			inner.setHeader(arg0, arg1);
		}

		public void setIntHeader(String arg0, int arg1) {
			inner.setIntHeader(arg0, arg1);
		}

		public void setLocale(Locale arg0) {
			inner.setLocale(arg0);
		}

		public void setStatus(int arg0, String arg1) {
			inner.setStatus(arg0, arg1);
		}

		public void setStatus(int arg0) {
			inner.setStatus(arg0);
		}
	  }

	  @Override
	  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
		  if (response instanceof HttpServletResponse ){//&& response.getContentType().startsWith("text/html")) {
			  HttpServletResponse resp = (HttpServletResponse) response;

			  //add configured security related headers
			  if ( xssProtection != null && !resp.containsHeader("X-XSS-Protection") )
				  resp.setHeader("X-XSS-Protection", xssProtection);
			  if ( frameOptions != null && !resp.containsHeader("X-Frame-Options")  )
				  resp.setHeader("X-Frame-Options", frameOptions);
			  if ( contentTypeOptions != null && !resp.containsHeader("X-Content-Type-Options")  )
				  resp.setHeader("X-Content-Type-Options", contentTypeOptions);
		  }

		  //add an response wrapper
		  //the response wrapper is responsible for re-adding the security
		  //headers if ruby is use (in which case the response is reset)
		  if (response instanceof HttpServletResponse )
			  chain.doFilter(request, new InnerResponse((HttpServletResponse)response));
		  else
			  chain.doFilter(request, response);
	  }

	  @Override
	  public void destroy() {
	  }
}
