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
import LocalizationContainer from '../components/LocalizationContainer';
import MigrationContainer from '../components/MigrationContainer';
import App from '../components/App';
import GlobalContainer from '../components/GlobalContainer';
import SimpleContainer from '../components/SimpleContainer';
import Landing from '../components/Landing';
import ProjectContainer from '../components/ProjectContainer';
import ProjectAdminContainer from '../components/ProjectAdminContainer';
import ProjectPageExtension from '../components/extensions/ProjectPageExtension';
import ProjectAdminPageExtension from '../components/extensions/ProjectAdminPageExtension';
import ViewDashboard from '../components/extensions/ViewDashboard';
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
import componentIssuesRoutes from '../../apps/component-issues/routes';
import componentMeasuresRoutes from '../../apps/component-measures/routes';
import customMeasuresRoutes from '../../apps/custom-measures/routes';
import groupsRoutes from '../../apps/groups/routes';
import issuesRoutes from '../../apps/issues/routes';
import metricsRoutes from '../../apps/metrics/routes';
import overviewRoutes from '../../apps/overview/routes';
import organizationsRouters from '../../apps/organizations/routes';
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
          path="/dashboard/index/:key"
          onEnter={(nextState, replace) => {
            replace({ pathname: '/dashboard', query: { id: nextState.params.key } });
          }}
        />

        <Route path="markdown/help" component={MarkdownHelp} />

        <Route component={LocalizationContainer}>
          <Route component={SimpleContainer}>
            <Route path="maintenance">{maintenanceRoutes}</Route>
            <Route path="setup">{setupRoutes}</Route>
          </Route>

          <Route component={MigrationContainer}>
            <Route component={SimpleContainer}>
              <Route path="/sessions">{sessionsRoutes}</Route>
            </Route>

            <Route path="/" component={App}>

              <IndexRoute component={Landing} />

              <Route component={GlobalContainer}>
                <Route path="about">{aboutRoutes}</Route>
                <Route path="account">{accountRoutes}</Route>
                <Route
                  path="codingrules"
                  onEnter={(nextState, replace) => {
                    replace('/coding_rules' + window.location.hash);
                  }}
                />
                <Route path="coding_rules">{codingRulesRoutes}</Route>
                <Route path="component">{componentRoutes}</Route>
                <Route path="extension/:pluginKey/:extensionKey" component={GlobalPageExtension} />
                <Route path="issues">{issuesRoutes}</Route>
                <Route path="organizations">{organizationsRouters}</Route>
                <Route path="projects">{projectsRoutes}</Route>
                <Route path="quality_gates">{qualityGatesRoutes}</Route>
                <Route path="profiles">{qualityProfilesRoutes}</Route>
                <Route path="web_api">{webAPIRoutes}</Route>

                <Route component={ProjectContainer}>
                  <Route path="code">{codeRoutes}</Route>
                  <Route path="component_issues">{componentIssuesRoutes}</Route>
                  <Route path="component_measures">{componentMeasuresRoutes}</Route>
                  <Route path="custom_measures">{customMeasuresRoutes}</Route>
                  <Route path="dashboard">{overviewRoutes}</Route>
                  <Redirect from="governance" to="/view" />
                  <Route path="project">
                    <Route path="activity">{projectActivityRoutes}</Route>
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
                    <Route path="background_tasks">{backgroundTasksRoutes}</Route>
                    <Route path="settings">{settingsRoutes}</Route>
                    {projectAdminRoutes}
                  </Route>
                  <Route path="project_roles">{projectPermissionsRoutes}</Route>
                  <Route path="view" component={ViewDashboard} />
                </Route>

                <Route component={AdminContainer}>
                  <Route
                    path="admin/extension/:pluginKey/:extensionKey"
                    component={GlobalAdminPageExtension}
                  />
                  <Route path="background_tasks">{backgroundTasksRoutes}</Route>
                  <Route path="groups">{groupsRoutes}</Route>
                  <Route path="metrics">{metricsRoutes}</Route>
                  <Route path="permission_templates">{permissionTemplatesRoutes}</Route>
                  <Route path="projects_admin">{projectsAdminRoutes}</Route>
                  <Route path="roles/global">{globalPermissionsRoutes}</Route>
                  <Route path="settings">{settingsRoutes}</Route>
                  <Route path="system">{systemRoutes}</Route>
                  <Route path="updatecenter">{updateCenterRoutes}</Route>
                  <Route path="users">{usersRoutes}</Route>
                </Route>
              </Route>

              <Route path="not_found" component={NotFound} />
              <Route path="*" component={NotFound} />
            </Route>
          </Route>
        </Route>
      </Router>
    </Provider>,
    el
  );
};

export default startReactApp;
