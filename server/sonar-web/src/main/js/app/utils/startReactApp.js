/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { render } from 'react-dom';
import { Router, Route, IndexRoute, Redirect } from 'react-router';
import { Provider } from 'react-redux';
import DefaultHelmetContainer from '../components/DefaultHelmetContainer';
import LocalizationContainer from '../components/LocalizationContainer';
import MigrationContainer from '../components/MigrationContainer';
import App from '../components/App';
import GlobalContainer from '../components/GlobalContainer';
import SimpleContainer from '../components/SimpleContainer';
import SimpleSessionsContainer from '../../apps/sessions/components/SimpleSessionsContainer';
import Landing from '../components/Landing';
import ProjectContainer from '../components/ProjectContainer';
import ProjectAdminContainer from '../components/ProjectAdminContainer';
import ProjectPageExtension from '../components/extensions/ProjectPageExtension';
import ProjectAdminPageExtension from '../components/extensions/ProjectAdminPageExtension';
import ViewDashboard from '../components/extensions/ViewDashboard';
import PortfoliosPage from '../components/extensions/PortfoliosPage';
import AdminContainer from '../components/AdminContainer';
import GlobalPageExtension from '../components/extensions/GlobalPageExtension';
import GlobalAdminPageExtension from '../components/extensions/GlobalAdminPageExtension';
import MarkdownHelp from '../components/MarkdownHelp';
import NotFound from '../components/NotFound';
import aboutRoutes from '../../apps/about/routes';
import accountRoutes from '../../apps/account/routes';
import backgroundTasksRoutes from '../../apps/background-tasks/routes';
import codeRoutes from '../../apps/code/routes';
import codingRulesRoutes from '../../apps/coding-rules/routes';
import componentRoutes from '../../apps/component/routes';
import componentMeasuresRoutes from '../../apps/component-measures/routes';
import customMeasuresRoutes from '../../apps/custom-measures/routes';
import groupsRoutes from '../../apps/groups/routes';
import issuesRoutes from '../../apps/issues/routes';
import metricsRoutes from '../../apps/metrics/routes';
import overviewRoutes from '../../apps/overview/routes';
import organizationsRoutes from '../../apps/organizations/routes';
import permissionTemplatesRoutes from '../../apps/permission-templates/routes';
import projectActivityRoutes from '../../apps/projectActivity/routes';
import projectAdminRoutes from '../../apps/project-admin/routes';
import projectsRoutes from '../../apps/projects/routes';
import projectsAdminRoutes from '../../apps/projects-admin/routes';
import qualityGatesRoutes from '../../apps/quality-gates/routes';
import qualityProfilesRoutes from '../../apps/quality-profiles/routes';
import sessionsRoutes from '../../apps/sessions/routes';
import settingsRoutes from '../../apps/settings/routes';
import systemRoutes from '../../apps/system/routes';
import updateCenterRoutes from '../../apps/update-center/routes';
import usersRoutes from '../../apps/users/routes';
import webAPIRoutes from '../../apps/web-api/routes';
import { maintenanceRoutes, setupRoutes } from '../../apps/maintenance/routes';
import { globalPermissionsRoutes, projectPermissionsRoutes } from '../../apps/permissions/routes';
import getStore from './getStore';
import getHistory from './getHistory';

function handleUpdate() {
  const { action } = this.state.location;

  if (action === 'PUSH') {
    window.scrollTo(0, 0);
  }
}

const startReactApp = () => {
  const el = document.getElementById('content');

  const history = getHistory();
  const store = getStore();

  render(
    <Provider store={store}>
      <Router history={history} onUpdate={handleUpdate}>
        <Route
          path="/account/issues"
          onEnter={(_, replace) => {
            replace({ pathname: '/issues', query: { myIssues: 'true', resolved: 'false' } });
          }}
        />

        <Route
          path="/codingrules"
          onEnter={(nextState, replace) => {
            replace('/coding_rules' + window.location.hash);
          }}
        />

        <Route
          path="/dashboard/index/:key"
          onEnter={(nextState, replace) => {
            replace({ pathname: '/dashboard', query: { id: nextState.params.key } });
          }}
        />

        <Route
          path="/issues/search"
          onEnter={(nextState, replace) => {
            replace('/issues' + window.location.hash);
          }}
        />

        <Redirect from="/component/index" to="/component" />
        <Redirect from="/component_issues" to="/project/issues" />
        <Redirect from="/dashboard/index" to="/dashboard" />
        <Redirect from="/governance" to="/view" />
        <Redirect from="/extension/governance/portfolios" to="/portfolios" />
        <Redirect from="/profiles/index" to="/profiles" />
        <Redirect from="/quality_gates/index" to="/quality_gates" />
        <Redirect from="/settings/index" to="/settings" />
        <Redirect from="/system/index" to="/system" />

        <Route path="markdown/help" component={MarkdownHelp} />

        <Route component={DefaultHelmetContainer}>
          <Route component={LocalizationContainer}>
            <Route component={SimpleContainer}>
              <Route path="maintenance">{maintenanceRoutes}</Route>
              <Route path="setup">{setupRoutes}</Route>
            </Route>

            <Route component={MigrationContainer}>
              <Route component={SimpleSessionsContainer}>
                <Route path="/sessions">{sessionsRoutes}</Route>
              </Route>

              <Route path="/" component={App}>

                <IndexRoute component={Landing} />

                <Route component={GlobalContainer}>
                  <Route path="about" childRoutes={aboutRoutes} />
                  <Route path="account" childRoutes={accountRoutes} />
                  <Route path="coding_rules" childRoutes={codingRulesRoutes} />
                  <Route path="component" childRoutes={componentRoutes} />
                  <Route
                    path="extension/:pluginKey/:extensionKey"
                    component={GlobalPageExtension}
                  />
                  <Route path="issues" childRoutes={issuesRoutes} />
                  <Route path="organizations" childRoutes={organizationsRoutes} />
                  <Route path="projects" childRoutes={projectsRoutes} />
                  <Route path="quality_gates" childRoutes={qualityGatesRoutes} />
                  <Route path="portfolios" component={PortfoliosPage} />
                  <Route path="profiles" childRoutes={qualityProfilesRoutes} />
                  <Route path="web_api" childRoutes={webAPIRoutes} />

                  <Route component={ProjectContainer}>
                    <Route path="code" childRoutes={codeRoutes} />
                    <Route path="component_measures" childRoutes={componentMeasuresRoutes} />
                    <Route path="custom_measures" childRoutes={customMeasuresRoutes} />
                    <Route path="dashboard" childRoutes={overviewRoutes} />
                    <Route path="project">
                      <Route path="activity" childRoutes={projectActivityRoutes} />
                      <Route path="admin" component={ProjectAdminContainer}>
                        <Route
                          path="extension/:pluginKey/:extensionKey"
                          component={ProjectAdminPageExtension}
                        />
                      </Route>
                      <Redirect from="extension/governance/governance" to="/view" />
                      <Route
                        path="extension/:pluginKey/:extensionKey"
                        component={ProjectPageExtension}
                      />
                      <Route path="background_tasks" childRoutes={backgroundTasksRoutes} />
                      <Route path="issues" childRoutes={issuesRoutes} />
                      <Route path="settings" childRoutes={settingsRoutes} />
                      {projectAdminRoutes}
                    </Route>
                    <Route path="project_roles" childRoutes={projectPermissionsRoutes} />
                    <Route path="view" component={ViewDashboard} />
                  </Route>

                  <Route component={AdminContainer}>
                    <Route
                      path="admin/extension/:pluginKey/:extensionKey"
                      component={GlobalAdminPageExtension}
                    />
                    <Route path="background_tasks" childRoutes={backgroundTasksRoutes} />
                    <Route path="groups" childRoutes={groupsRoutes} />
                    <Route path="metrics" childRoutes={metricsRoutes} />
                    <Route path="permission_templates" childRoutes={permissionTemplatesRoutes} />
                    <Route path="projects_admin" childRoutes={projectsAdminRoutes} />
                    <Route path="roles/global" childRoutes={globalPermissionsRoutes} />
                    <Route path="settings" childRoutes={settingsRoutes} />
                    <Route path="system" childRoutes={systemRoutes} />
                    <Route path="updatecenter" childRoutes={updateCenterRoutes} />
                    <Route path="users" childRoutes={usersRoutes} />
                  </Route>
                </Route>

                <Route path="not_found" component={NotFound} />
                <Route path="*" component={NotFound} />
              </Route>
            </Route>
          </Route>
        </Route>
      </Router>
    </Provider>,
    el
  );
};

export default startReactApp;
