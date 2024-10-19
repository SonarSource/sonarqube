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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.PlatformImpl;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CustomerAdminFilter implements Filter {

    private static final Set<String> includeUrls = new HashSet<>(Arrays.asList("/api/system/info", "/api/ce/task",
            "/api/ce/task_types", "/api/ce/worker_count", "/api/ce/activity", "/api/organizations/search",
            "/api/ce/activity_status", "/api/settings/set", "/api/settings/reset", "/api/new_code_periods/set"));
    private final Platform platform;

    public CustomerAdminFilter() {
        this.platform = PlatformImpl.getInstance();
    }

    @VisibleForTesting
    CustomerAdminFilter(Platform platform) {
        this.platform = platform;
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpRequest request = (HttpRequest) servletRequest;
        HttpResponse response = (HttpResponse) servletResponse;
        String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");
        if (includeUrls.contains(path)) {
            DbClient dbClient = platform.getContainer().getComponentByType(DbClient.class);
            DbSession dbSession = dbClient.openSession(false);
            OrganizationDto organization = dbClient.organizationDao().getDefaultOrganization(dbSession);
            ThreadLocalUserSession threadLocalUserSession = getComponent(ThreadLocalUserSession.class);
            UserSession userSession = threadLocalUserSession.get();
            if (userSession.hasPermission(OrganizationPermission.ADMINISTER_CUSTOMER,
                    organization.getUuid())) {
                Optional<JwtHttpHandler.Token> tokenOpt = getComponent(JwtHttpHandler.class)
                        .getToken(request, response);
                if (tokenOpt.isPresent()) {
                    UserDto userDto = tokenOpt.get().getUserDto();
                    userDto.setRoot(true);
                    UserSessionFactory userSessionFactory = getComponent(UserSessionFactory.class);
                    threadLocalUserSession.set(userSessionFactory.create(userDto, true /* TODO */));
                }

            }
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    private <T> T getComponent(Class<T> type) {
        return platform.getContainer().getComponentByType(type);
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}