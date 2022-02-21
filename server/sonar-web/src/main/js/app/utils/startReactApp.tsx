/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
/* eslint-disable react/jsx-sort-props */
import { Location } from 'history';
import { pick } from 'lodash';
import * as React from 'react';
import ReactDom, { render } from 'react-dom';
import { HelmetProvider } from 'react-helmet-async';
import { IntlProvider } from 'react-intl';
import { Provider } from 'react-redux';
import { IndexRoute, Redirect, Route, RouteConfig, RouteProps, Router } from 'react-router';
import accountRoutes from '../../apps/account/routes';
import auditLogsRoutes from '../../apps/audit-logs/routes';
import backgroundTasksRoutes from '../../apps/background-tasks/routes';
import codeRoutes from '../../apps/code/routes';
import codingRulesRoutes from '../../apps/coding-rules/routes';
import componentMeasuresRoutes from '../../apps/component-measures/routes';
import documentationRoutes from '../../apps/documentation/routes';
import groupsRoutes from '../../apps/groups/routes';
import Issues from '../../apps/issues/components/AppContainer';
import { maintenanceRoutes, setupRoutes } from '../../apps/maintenance/routes';
import marketplaceRoutes from '../../apps/marketplace/routes';
import overviewRoutes from '../../apps/overview/routes';
import permissionTemplatesRoutes from '../../apps/permission-templates/routes';
import { globalPermissionsRoutes, projectPermissionsRoutes } from '../../apps/permissions/routes';
import projectActivityRoutes from '../../apps/projectActivity/routes';
import projectBaselineRoutes from '../../apps/projectBaseline/routes';
import projectBranchesRoutes from '../../apps/projectBranches/routes';
import projectDumpRoutes from '../../apps/projectDump/routes';
import projectQualityGateRoutes from '../../apps/projectQualityGate/routes';
import projectQualityProfilesRoutes from '../../apps/projectQualityProfiles/routes';
import projectsRoutes from '../../apps/projects/routes';
import projectsManagementRoutes from '../../apps/projectsManagement/routes';
import qualityGatesRoutes from '../../apps/quality-gates/routes';
import qualityProfilesRoutes from '../../apps/quality-profiles/routes';
import sessionsRoutes from '../../apps/sessions/routes';
import settingsRoutes from '../../apps/settings/routes';
import systemRoutes from '../../apps/system/routes';
import tutorialsRoutes from '../../apps/tutorials/routes';
import usersRoutes from '../../apps/users/routes';
import webAPIRoutes from '../../apps/web-api/routes';
import webhooksRoutes from '../../apps/webhooks/routes';
import withIndexationGuard from '../../components/hoc/withIndexationGuard';
import { lazyLoadComponent } from '../../components/lazyLoadComponent';
import getHistory from '../../helpers/getHistory';
import { AppState, CurrentUser } from '../../types/types';
import App from '../components/App';
import AppStateContextProvider from '../components/app-state/AppStateContextProvider';
import GlobalContainer from '../components/GlobalContainer';
import { PageContext } from '../components/indexation/PageUnavailableDueToIndexation';
import MigrationContainer from '../components/MigrationContainer';
import NonAdminPagesContainer from '../components/NonAdminPagesContainer';
import getStore from './getStore';

/*
 * Expose dependencies to extensions
 */
function attachToGlobal() {
  window.React = React;
  window.ReactDOM = ReactDom;
}

function handleUpdate(this: { state: { location: Location } }) {
  const { action } = this.state.location;

  if (action === 'PUSH') {
    window.scrollTo(0, 0);
  }
}

// this is not an official api
export const RouteWithChildRoutes = Route as React.ComponentClass<
  RouteProps & { childRoutes: RouteConfig }
>;

function renderRedirects() {
  return (
    <>
      <Route
        path="/account/issues"
        onEnter={(_, replace) => {
          replace({ pathname: '/issues', query: { myIssues: 'true', resolved: 'false' } });
        }}
      />

      <Route
        path="/codingrules"
        onEnter={(_, replace) => {
          replace(`/coding_rules${window.location.hash}`);
        }}
      />

      <Route
        path="/dashboard/index/:key"
        onEnter={(nextState, replace) => {
          replace({ pathname: '/dashboard', query: { id: nextState.params.key } });
        }}
      />

      <Route
        path="/application/console"
        onEnter={(nextState, replace) => {
          replace({
            pathname: '/project/admin/extension/developer-server/application-console',
            query: { id: nextState.location.query.id }
          });
        }}
      />

      <Route
        path="/application/settings"
        onEnter={(nextState, replace) => {
          replace({
            pathname: '/project/admin/extension/governance/application_report',
            query: { id: nextState.location.query.id }
          });
        }}
      />

      <Route
        path="/issues/search"
        onEnter={(_, replace) => {
          replace(`/issues${window.location.hash}`);
        }}
      />

      <Redirect from="/admin" to="/admin/settings" />
      <Redirect from="/background_tasks" to="/admin/background_tasks" />
      <Redirect from="/component/index" to="/component" />
      <Redirect from="/component_issues" to="/project/issues" />
      <Redirect from="/dashboard/index" to="/dashboard" />
      <Redirect
        from="/documentation/analysis/languages/vb"
        to="/documentation/analysis/languages/vbnet/"
      />
      <Redirect from="/governance" to="/portfolio" />
      <Redirect from="/groups" to="/admin/groups" />
      <Redirect from="/extension/governance/portfolios" to="/portfolios" />
      <Redirect from="/permission_templates" to="/admin/permission_templates" />
      <Redirect from="/profiles/index" to="/profiles" />
      <Redirect from="/projects_admin" to="/admin/projects_management" />
      <Redirect from="/quality_gates/index" to="/quality_gates" />
      <Redirect from="/roles/global" to="/admin/permissions" />
      <Redirect from="/admin/roles/global" to="/admin/permissions" />
      <Redirect from="/settings" to="/admin/settings" />
      <Redirect from="/settings/encryption" to="/admin/settings/encryption" />
      <Redirect from="/settings/index" to="/admin/settings" />
      <Redirect from="/sessions/login" to="/sessions/new" />
      <Redirect from="/system" to="/admin/system" />
      <Redirect from="/system/index" to="/admin/system" />
      <Redirect from="/view" to="/portfolio" />
      <Redirect from="/users" to="/admin/users" />
      <Redirect from="/onboarding" to="/projects/create" />
      <Redirect from="markdown/help" to="formatting/help" />
    </>
  );
}

function renderComponentRoutes() {
  return (
    <Route component={lazyLoadComponent(() => import('../components/ComponentContainer'))}>
      {/* This container is a catch-all for all non-admin pages */}
      <Route component={NonAdminPagesContainer}>
        <RouteWithChildRoutes path="code" childRoutes={codeRoutes} />
        <RouteWithChildRoutes path="component_measures" childRoutes={componentMeasuresRoutes} />
        <RouteWithChildRoutes path="dashboard" childRoutes={overviewRoutes} />
        <Route
          path="portfolio"
          component={lazyLoadComponent(() => import('../components/extensions/PortfolioPage'))}
        />
        <RouteWithChildRoutes path="project/activity" childRoutes={projectActivityRoutes} />
        <Route
          path="project/extension/:pluginKey/:extensionKey"
          component={lazyLoadComponent(() =>
            import('../components/extensions/ProjectPageExtension')
          )}
        />
        <Route
          path="project/issues"
          component={Issues}
          onEnter={({ location: { query } }, replace) => {
            if (query.types) {
              if (query.types === 'SECURITY_HOTSPOT') {
                replace({
                  pathname: '/security_hotspots',
                  query: { ...pick(query, ['id', 'branch', 'pullRequest']), assignedToMe: false }
                });
              } else {
                query.types = query.types
                  .split(',')
                  .filter((type: string) => type !== 'SECURITY_HOTSPOT')
                  .join(',');
              }
            }
          }}
        />
        <Route
          path="security_hotspots"
          component={lazyLoadComponent(() =>
            import('../../apps/security-hotspots/SecurityHotspotsApp')
          )}
        />
        <RouteWithChildRoutes path="project/quality_gate" childRoutes={projectQualityGateRoutes} />
        <RouteWithChildRoutes
          path="project/quality_profiles"
          childRoutes={projectQualityProfilesRoutes}
        />
        <RouteWithChildRoutes path="tutorials" childRoutes={tutorialsRoutes} />
      </Route>
      <Route component={lazyLoadComponent(() => import('../components/ProjectAdminContainer'))}>
        <Route
          path="project/admin/extension/:pluginKey/:extensionKey"
          component={lazyLoadComponent(() =>
            import('../components/extensions/ProjectAdminPageExtension')
          )}
        />
        <RouteWithChildRoutes path="project/background_tasks" childRoutes={backgroundTasksRoutes} />
        <RouteWithChildRoutes path="project/baseline" childRoutes={projectBaselineRoutes} />
        <RouteWithChildRoutes path="project/branches" childRoutes={projectBranchesRoutes} />
        <RouteWithChildRoutes path="project/import_export" childRoutes={projectDumpRoutes} />
        <RouteWithChildRoutes path="project/settings" childRoutes={settingsRoutes} />
        <RouteWithChildRoutes path="project_roles" childRoutes={projectPermissionsRoutes} />
        <RouteWithChildRoutes path="project/webhooks" childRoutes={webhooksRoutes} />
        <Route
          path="project/deletion"
          component={lazyLoadComponent(() => import('../../apps/projectDeletion/App'))}
        />
        <Route
          path="project/links"
          component={lazyLoadComponent(() => import('../../apps/projectLinks/App'))}
        />
        <Route
          path="project/key"
          component={lazyLoadComponent(() => import('../../apps/projectKey/Key'))}
        />
      </Route>
    </Route>
  );
}

function renderAdminRoutes() {
  return (
    <Route component={lazyLoadComponent(() => import('../components/AdminContainer'))} path="admin">
      <Route
        path="extension/:pluginKey/:extensionKey"
        component={lazyLoadComponent(() =>
          import('../components/extensions/GlobalAdminPageExtension')
        )}
      />
      <RouteWithChildRoutes path="audit" childRoutes={auditLogsRoutes} />
      <RouteWithChildRoutes path="background_tasks" childRoutes={backgroundTasksRoutes} />
      <RouteWithChildRoutes path="groups" childRoutes={groupsRoutes} />
      <RouteWithChildRoutes path="permission_templates" childRoutes={permissionTemplatesRoutes} />
      <RouteWithChildRoutes path="permissions" childRoutes={globalPermissionsRoutes} />
      <RouteWithChildRoutes path="projects_management" childRoutes={projectsManagementRoutes} />
      <RouteWithChildRoutes path="settings" childRoutes={settingsRoutes} />
      <RouteWithChildRoutes path="system" childRoutes={systemRoutes} />
      <RouteWithChildRoutes path="marketplace" childRoutes={marketplaceRoutes} />
      <RouteWithChildRoutes path="users" childRoutes={usersRoutes} />
      <RouteWithChildRoutes path="webhooks" childRoutes={webhooksRoutes} />
    </Route>
  );
}

export default function startReactApp(lang: string, appState: AppState, currentUser?: CurrentUser) {
  attachToGlobal();

  const el = document.getElementById('content');

  const history = getHistory();
  const store = getStore(currentUser);

  render(
    <HelmetProvider>
      <Provider store={store}>
        <AppStateContextProvider appState={appState}>
          <IntlProvider defaultLocale={lang} locale={lang}>
            <Router history={history} onUpdate={handleUpdate}>
              {renderRedirects()}

              <Route
                path="formatting/help"
                component={lazyLoadComponent(() => import('../components/FormattingHelp'))}
              />

              <Route component={lazyLoadComponent(() => import('../components/SimpleContainer'))}>
                <Route path="maintenance">{maintenanceRoutes}</Route>
                <Route path="setup">{setupRoutes}</Route>
              </Route>

              <Route component={MigrationContainer}>
                <Route
                  component={lazyLoadComponent(() =>
                    import('../components/SimpleSessionsContainer')
                  )}>
                  <RouteWithChildRoutes path="/sessions" childRoutes={sessionsRoutes} />
                </Route>

                <Route path="/" component={App}>
                  <IndexRoute
                    component={lazyLoadComponent(() => import('../components/Landing'))}
                  />

                  <Route component={GlobalContainer}>
                    <RouteWithChildRoutes path="account" childRoutes={accountRoutes} />
                    <RouteWithChildRoutes path="coding_rules" childRoutes={codingRulesRoutes} />
                    <RouteWithChildRoutes path="documentation" childRoutes={documentationRoutes} />
                    <Route
                      path="extension/:pluginKey/:extensionKey"
                      component={lazyLoadComponent(() =>
                        import('../components/extensions/GlobalPageExtension')
                      )}
                    />
                    <Route
                      path="issues"
                      component={withIndexationGuard(Issues, PageContext.Issues)}
                    />
                    <RouteWithChildRoutes path="projects" childRoutes={projectsRoutes} />
                    <RouteWithChildRoutes path="quality_gates" childRoutes={qualityGatesRoutes} />
                    <Route
                      path="portfolios"
                      component={lazyLoadComponent(() =>
                        import('../components/extensions/PortfoliosPage')
                      )}
                    />
                    <RouteWithChildRoutes path="profiles" childRoutes={qualityProfilesRoutes} />
                    <RouteWithChildRoutes path="web_api" childRoutes={webAPIRoutes} />

                    {renderComponentRoutes()}

                    {renderAdminRoutes()}
                  </Route>
                  <Route
                    // We don't want this route to have any menu.
                    // That is why we can not have it under the accountRoutes
                    path="account/reset_password"
                    component={lazyLoadComponent(() => import('../components/ResetPassword'))}
                  />
                  <Route
                    // We don't want this route to have any menu. This is why we define it here
                    // rather than under the admin routes.
                    path="admin/change_admin_password"
                    component={lazyLoadComponent(() =>
                      import('../../apps/change-admin-password/ChangeAdminPasswordApp')
                    )}
                  />
                  <Route
                    // We don't want this route to have any menu. This is why we define it here
                    // rather than under the admin routes.
                    path="admin/plugin_risk_consent"
                    component={lazyLoadComponent(() => import('../components/PluginRiskConsent'))}
                  />
                  <Route
                    path="not_found"
                    component={lazyLoadComponent(() => import('../components/NotFound'))}
                  />
                  <Route
                    path="*"
                    component={lazyLoadComponent(() => import('../components/NotFound'))}
                  />
                </Route>
              </Route>
            </Router>
          </IntlProvider>
        </AppStateContextProvider>
      </Provider>
    </HelmetProvider>,
    el
  );
}
