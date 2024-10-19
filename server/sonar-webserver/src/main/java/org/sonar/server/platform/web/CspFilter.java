/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CspFilter implements Filter {

    private final List<String> cspHeaders = new ArrayList<>();
    private String defaultPolicies = null;
    private String codescanPolicies = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        cspHeaders.add("Content-Security-Policy");

        this.defaultPolicies = String.join("; ", getDefaultCspPolicies()).trim();
        this.codescanPolicies = String.join("; ", getCodescanCspPolicies()).trim();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        boolean isCodescan = request.getServerName().contains("codescan.io") || request.getServerName().contains("autorabit.com");
        String policies = isCodescan ? codescanPolicies : defaultPolicies;
        // Add policies to all HTTP headers
        for (String header : this.cspHeaders) {
            ((HttpServletResponse) response).setHeader(header, policies);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Not used
    }

    private List<String> getDefaultCspPolicies() {
        List<String> cspPolicies = new ArrayList<>();
        cspPolicies.add("default-src 'self'");
        cspPolicies.add("base-uri 'none'");
        cspPolicies.add("connect-src 'self' http: https:");
        cspPolicies.add("font-src 'self' data:");
        cspPolicies.add("img-src * data: blob:");
        cspPolicies.add("object-src 'none'");
        cspPolicies.add("script-src 'self'");
        cspPolicies.add("style-src 'self' 'unsafe-inline'");
        return cspPolicies;
    }

    private List<String> getCodescanCspPolicies() {
        List<String> cspPolicies = new ArrayList<>();

        // Directives not specified default to this one.
        cspPolicies.add("default-src 'self'");

        cspPolicies.add("base-uri 'none'");
        cspPolicies.add("img-src * data: blob:");
        cspPolicies.add("object-src 'none'");

        // Allow list for GoogleTagManager, Pendo, FullStory, Linkedin, GoogleAnalytics, Facebook, zdassets Scripts.
        cspPolicies.add("connect-src 'self' https://edge.fullstory.com https://rs.fullstory.com app.pendo.io "
                + "data.pendo.io pendo-static-6580644462460928.storage.googleapis.com http: https:");
        cspPolicies.add("font-src 'self' data:");
        cspPolicies.add(
                "script-src 'self' https://www.googletagmanager.com https://pendo-io-static.storage.googleapis.com "
                        + "pendo-static-6580644462460928.storage.googleapis.com "
                        + "https://app.pendo.io https://cdn.pendo.io https://data.pendo.io https://edge.fullstory.com "
                        + "https://rs.fullstory.com https://ssl.google-analytics.com https://static.zdassets.com "
                        + "https://connect.facebook.net https://snap.licdn.com 'unsafe-inline' 'unsafe-eval'");
        cspPolicies.add("style-src 'self' 'unsafe-inline' app.pendo.io cdn.pendo.io "
                + "pendo-static-6580644462460928.storage.googleapis.com");
        cspPolicies.add("worker-src 'none'");
        cspPolicies.add("frame-ancestors 'self' app.pendo.io");
        cspPolicies.add("frame-src 'self' app.pendo.io");
        cspPolicies.add("child-src 'self' app.pendo.io");
        return cspPolicies;
    }

}
