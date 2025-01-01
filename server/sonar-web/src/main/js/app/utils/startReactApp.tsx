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

import { ThemeProvider } from '@emotion/react';
import styled from '@emotion/styled';
import { EchoesProvider } from '@sonarsource/echoes-react';
import { QueryClientProvider } from '@tanstack/react-query';
import { createRoot } from 'react-dom/client';
import { Helmet, HelmetProvider } from 'react-helmet-async';
import { IntlShape, RawIntlProvider } from 'react-intl';
import {
  Outlet,
  Route,
  RouterProvider,
  createBrowserRouter,
  createRoutesFromElements,
} from 'react-router-dom';
import { ToastMessageContainer, lightTheme } from '~design-system';
import { lazyLoadComponent } from '~sonar-aligned/helpers/lazyLoadComponent';
import accountRoutes from '../../apps/account/routes';
import auditLogsRoutes from '../../apps/audit-logs/routes';
import backgroundTasksRoutes from '../../apps/background-tasks/routes';
import codeRoutes from '../../apps/code/routes';
import componentMeasuresRoutes from '../../apps/component-measures/routes';
import { dependenciesRoutes } from '../../apps/dependencies/routes';
import groupsRoutes from '../../apps/groups/routes';
import { globalIssuesRoutes, projectIssuesRoutes } from '../../apps/issues/routes';
import maintenanceRoutes from '../../apps/maintenance/routes';
import marketplaceRoutes from '../../apps/marketplace/routes';
import overviewRoutes from '../../apps/overview/routes';
import permissionTemplatesRoutes from '../../apps/permission-templates/routes';
import { globalPermissionsRoutes, projectPermissionsRoutes } from '../../apps/permissions/routes';
import projectActivityRoutes from '../../apps/projectActivity/routes';
import projectBranchesRoutes from '../../apps/projectBranches/routes';
import projectDeletionRoutes from '../../apps/projectDeletion/routes';
import projectDumpRoutes from '../../apps/projectDump/routes';
import projectInfoRoutes from '../../apps/projectInformation/routes';
import projectKeyRoutes from '../../apps/projectKey/routes';
import projectLinksRoutes from '../../apps/projectLinks/routes';
import projectNewCodeDefinitionRoutes from '../../apps/projectNewCode/routes';
import projectQualityGateRoutes from '../../apps/projectQualityGate/routes';
import projectQualityProfilesRoutes from '../../apps/projectQualityProfiles/routes';
import projectsRoutes from '../../apps/projects/routes';
import projectsManagementRoutes from '../../apps/projectsManagement/routes';
import qualityGatesRoutes from '../../apps/quality-gates/routes';
import qualityProfilesRoutes from '../../apps/quality-profiles/routes';
import securityHotspotsRoutes from '../../apps/security-hotspots/routes';
import sessionsRoutes from '../../apps/sessions/routes';
import settingsRoutes from '../../apps/settings/routes';
import systemRoutes from '../../apps/system/routes';
import tutorialsRoutes from '../../apps/tutorials/routes';
import usersRoutes from '../../apps/users/routes';
import webAPIRoutesV2 from '../../apps/web-api-v2/routes';
import webAPIRoutes from '../../apps/web-api/routes';
import webhooksRoutes from '../../apps/webhooks/routes';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { queryClient } from '../../queries/queryClient';
import { AppState } from '../../types/appstate';
import { Feature } from '../../types/features';
import { CurrentUser } from '../../types/users';
import AdminContainer from '../components/AdminContainer';
import App from '../components/App';
import ComponentContainer from '../components/ComponentContainer';
import DocumentationRedirect from '../components/DocumentationRedirect';
import GlobalContainer from '../components/GlobalContainer';
import Landing from '../components/Landing';
import MigrationContainer from '../components/MigrationContainer';
import NonAdminPagesContainer from '../components/NonAdminPagesContainer';
import NotFound from '../components/NotFound';
import ProjectAdminContainer from '../components/ProjectAdminContainer';
import SimpleContainer from '../components/SimpleContainer';
import { DEFAULT_APP_STATE } from '../components/app-state/AppStateContext';
import AppStateContextProvider from '../components/app-state/AppStateContextProvider';
import {
  AvailableFeaturesContext,
  DEFAULT_AVAILABLE_FEATURES,
} from '../components/available-features/AvailableFeaturesContext';
import CurrentUserContextProvider from '../components/current-user/CurrentUserContextProvider';
import GlobalAdminPageExtension from '../components/extensions/GlobalAdminPageExtension';
import GlobalPageExtension from '../components/extensions/GlobalPageExtension';
import PortfolioPage from '../components/extensions/PortfolioPage';
import PortfoliosPage from '../components/extensions/PortfoliosPage';
import ProjectAdminPageExtension from '../components/extensions/ProjectAdminPageExtension';
import ProjectPageExtension from '../components/extensions/ProjectPageExtension';
import { GlobalStyles } from '../styles/GlobalStyles';
import exportModulesAsGlobals from './exportModulesAsGlobals';
import organizationsRoutes from '../../apps/organizations/routes';
import { Organization } from "../../types/types";
import Home from "../components/Home";

function renderComponentRoutes() {
  return (
    <Route element={<ComponentContainer />}>
      {/* This container is a catch-all for all non-admin pages */}
      <Route element={<NonAdminPagesContainer />}>
        {codeRoutes()}
        {componentMeasuresRoutes()}
        {overviewRoutes()}
        <Route path="portfolio" element={<PortfolioPage />} />
        {projectActivityRoutes()}
        <Route
          path="project/extension/:pluginKey/:extensionKey"
          element={<ProjectPageExtension />}
        />
        {projectIssuesRoutes()}
        {dependenciesRoutes()}
        {securityHotspotsRoutes()}
        {projectQualityGateRoutes()}
        {projectQualityProfilesRoutes()}
        {projectInfoRoutes()}

        {tutorialsRoutes()}
      </Route>
      <Route element={<ProjectAdminContainer />}>
        <Route path="project">
          <Route
            path="admin/extension/:pluginKey/:extensionKey"
            element={<ProjectAdminPageExtension />}
          />
          {backgroundTasksRoutes()}
          {projectNewCodeDefinitionRoutes()}
          {projectBranchesRoutes()}
          {projectDumpRoutes()}
          {settingsRoutes()}
          {webhooksRoutes()}

          {projectDeletionRoutes()}
          {projectLinksRoutes()}
          {projectKeyRoutes()}
        </Route>
        {projectPermissionsRoutes()}
      </Route>
    </Route>
  );
}

function renderAdminRoutes() {
  return (
    <Route path="admin" element={<AdminContainer />}>
      <Route path="extension/:pluginKey/:extensionKey" element={<GlobalAdminPageExtension />} />
      {settingsRoutes()}
      {auditLogsRoutes()}
      {backgroundTasksRoutes()}
      {groupsRoutes()}
      {permissionTemplatesRoutes()}
      {globalPermissionsRoutes()}
      {projectsManagementRoutes()}
      {systemRoutes()}
      {marketplaceRoutes()}
      {usersRoutes()}
      {webhooksRoutes()}
    </Route>
  );
}

function renderRedirects() {
  return (
    <>
      {/*
       * This redirect enables analyzers and PDFs to link to the correct version of the
       * documentation without having to compute the direct links themselves (DRYer).
       */}
      <Route path="/documentation/*" element={<DocumentationRedirect />} />
    </>
  );
}

const FormattingHelp = lazyLoadComponent(() => import('../components/FormattingHelp'));
const SonarLintConnection = lazyLoadComponent(() => import('../components/SonarLintConnection'));
const ResetPassword = lazyLoadComponent(() => import('../components/ResetPassword'));
const ChangeAdminPasswordApp = lazyLoadComponent(
  () => import('../../apps/change-admin-password/ChangeAdminPasswordApp'),
);
const PluginRiskConsent = lazyLoadComponent(() => import('../components/PluginRiskConsent'));

const router = createBrowserRouter(
  createRoutesFromElements(
    // Wrapper to pass toaster container under router context
    // this way we can use router context in toast message, for example render links
    <Route
      element={
        <>
          <ToastMessageContainer />
          <Outlet />
        </>
      }
    >
      {renderRedirects()}

      <Route path="formatting/help" element={<FormattingHelp />} />

      <Route element={<SimpleContainer />}>{maintenanceRoutes()}</Route>

      <Route element={<MigrationContainer />}>
        {sessionsRoutes()}

        <Route path="/" element={<App />}>
          <Route index element={<Landing />} />

          <Route element={<GlobalContainer />}>
            {accountRoutes()}

            <Route path="extension/:pluginKey/:extensionKey" element={<GlobalPageExtension />} />

            {globalIssuesRoutes()}

            {organizationsRoutes()}

            {projectsRoutes()}

            {qualityGatesRoutes()}
            {qualityProfilesRoutes()}

            <Route path="sonarlint/auth" element={<SonarLintConnection />} />

            {webAPIRoutes()}
            {webAPIRoutesV2()}

            {renderComponentRoutes()}

            {renderAdminRoutes()}
          </Route>
          <Route
            // We don't want this route to have any menu.
            // That is why we can not have it under the accountRoutes
            path="account/reset_password"
            element={<ResetPassword />}
          />

          <Route
            // We don't want this route to have any menu. This is why we define it here
            // rather than under the admin routes.
            path="admin/change_admin_password"
            element={<ChangeAdminPasswordApp />}
          />

          <Route
            // We don't want this route to have any menu. This is why we define it here
            // rather than under the admin routes.
            path="admin/plugin_risk_consent"
            element={<PluginRiskConsent />}
          />

          <Route element={<SimpleContainer />}>
            <Route path="home" element={<Home />} />
            <Route path="not_found" element={<NotFound />} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Route>
      </Route>
    </Route>,
  ),
  { basename: getBaseUrl() },
);

export default function startReactApp(
  l10nBundle: IntlShape,
  userOrganizations?: Organization[],
  currentUser?: CurrentUser,
  appState?: AppState,
  availableFeatures?: Feature[],
) {
  exportModulesAsGlobals();

  const el = document.getElementById('content');
  const root = createRoot(el as HTMLElement);

  root.render(
    <HelmetProvider>
      <AppStateContextProvider appState={appState ?? DEFAULT_APP_STATE}>
        <AvailableFeaturesContext.Provider value={availableFeatures ?? DEFAULT_AVAILABLE_FEATURES}>
          <CurrentUserContextProvider currentUser={currentUser} userOrganizations={userOrganizations}>
            <RawIntlProvider value={l10nBundle}>
              <ThemeProvider theme={lightTheme}>
                <QueryClientProvider client={queryClient}>
                  <GlobalStyles />
                  <Helmet titleTemplate={translate('page_title.template.default')} />
                  <StackContext>
                    <EchoesProvider>
                      <RouterProvider router={router} />
                    </EchoesProvider>
                  </StackContext>
                </QueryClientProvider>
              </ThemeProvider>
            </RawIntlProvider>
          </CurrentUserContextProvider>
        </AvailableFeaturesContext.Provider>
      </AppStateContextProvider>
    </HelmetProvider>,
  );
}

/*
 * This ensures tooltips and other "floating" elements appended to the body are placed on top
 * of the rest of the UI.
 */
const StackContext = styled.div`
  z-index: 0;
  position: relative;
`;
