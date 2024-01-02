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
import { render, RenderResult } from '@testing-library/react';
import * as React from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { IntlProvider } from 'react-intl';
import { MemoryRouter, Outlet, parsePath, Route, Routes } from 'react-router-dom';
import AdminContext from '../app/components/AdminContext';
import AppStateContextProvider from '../app/components/app-state/AppStateContextProvider';
import { AvailableFeaturesContext } from '../app/components/available-features/AvailableFeaturesContext';
import { ComponentContext } from '../app/components/componentContext/ComponentContext';
import CurrentUserContextProvider from '../app/components/current-user/CurrentUserContextProvider';
import GlobalMessagesContainer from '../app/components/GlobalMessagesContainer';
import IndexationContextProvider from '../app/components/indexation/IndexationContextProvider';
import { LanguagesContext } from '../app/components/languages/LanguagesContext';
import { MetricsContext } from '../app/components/metrics/MetricsContext';
import { useLocation } from '../components/hoc/withRouter';
import { AppState } from '../types/appstate';
import { ComponentContextShape } from '../types/component';
import { Feature } from '../types/features';
import { Dict, Extension, Languages, Metric, SysStatus } from '../types/types';
import { CurrentUser } from '../types/users';
import { DEFAULT_METRICS } from './mocks/metrics';
import { mockAppState, mockCurrentUser } from './testMocks';

export interface RenderContext {
  metrics?: Dict<Metric>;
  appState?: AppState;
  languages?: Languages;
  currentUser?: CurrentUser;
  navigateTo?: string;
  featureList?: Feature[];
}

export function renderAppWithAdminContext(
  indexPath: string,
  routes: () => JSX.Element,
  context: RenderContext = {},
  overrides: { systemStatus?: SysStatus; adminPages?: Extension[] } = {}
): RenderResult {
  function MockAdminContainer() {
    return (
      <AdminContext.Provider
        value={{
          fetchSystemStatus: () => {
            /*noop*/
          },
          fetchPendingPlugins: () => {
            /*noop*/
          },
          pendingPlugins: { installing: [], removing: [], updating: [] },
          systemStatus: overrides.systemStatus ?? 'UP',
        }}
      >
        <Outlet
          context={{
            adminPages: overrides.adminPages ?? [],
          }}
        />
      </AdminContext.Provider>
    );
  }

  return renderRoutedApp(
    <Route element={<MockAdminContainer />} path="admin">
      {routes()}
    </Route>,
    indexPath,
    context
  );
}

export function renderComponent(component: React.ReactElement, pathname = '/') {
  function Wrapper({ children }: { children: React.ReactElement }) {
    return (
      <IntlProvider defaultLocale="en" locale="en">
        <MemoryRouter initialEntries={[pathname]}>
          <Routes>
            <Route path="*" element={children} />
          </Routes>
        </MemoryRouter>
      </IntlProvider>
    );
  }

  return render(component, { wrapper: Wrapper });
}

export function renderAppWithComponentContext(
  indexPath: string,
  routes: () => JSX.Element,
  context: RenderContext = {},
  componentContext?: Partial<ComponentContextShape>
) {
  function MockComponentContainer() {
    return (
      <ComponentContext.Provider
        value={{
          branchLikes: [],
          onBranchesChange: jest.fn(),
          onComponentChange: jest.fn(),
          ...componentContext,
        }}
      >
        <Outlet />
      </ComponentContext.Provider>
    );
  }

  return renderRoutedApp(
    <Route element={<MockComponentContainer />}>{routes()}</Route>,
    indexPath,
    context
  );
}

export function renderApp(
  indexPath: string,
  component: JSX.Element,
  context: RenderContext = {}
): RenderResult {
  return renderRoutedApp(<Route path={indexPath} element={component} />, indexPath, context);
}

export function renderAppRoutes(
  indexPath: string,
  routes: () => JSX.Element,
  context?: RenderContext
): RenderResult {
  return renderRoutedApp(routes(), indexPath, context);
}

export function CatchAll() {
  const location = useLocation();

  return <div>{`${location.pathname}${location.search}`}</div>;
}

function renderRoutedApp(
  children: React.ReactElement,
  indexPath: string,
  {
    currentUser = mockCurrentUser(),
    navigateTo = indexPath,
    metrics = DEFAULT_METRICS,
    appState = mockAppState(),
    featureList = [],
    languages = {},
  }: RenderContext = {}
): RenderResult {
  const path = parsePath(navigateTo);
  path.pathname = `/${path.pathname}`;

  return render(
    <HelmetProvider context={{}}>
      <IntlProvider defaultLocale="en" locale="en">
        <MetricsContext.Provider value={metrics}>
          <LanguagesContext.Provider value={languages}>
            <AvailableFeaturesContext.Provider value={featureList}>
              <CurrentUserContextProvider currentUser={currentUser}>
                <AppStateContextProvider appState={appState}>
                  <IndexationContextProvider>
                    <GlobalMessagesContainer />
                    <MemoryRouter initialEntries={[path]}>
                      <Routes>
                        {children}
                        <Route path="*" element={<CatchAll />} />
                      </Routes>
                    </MemoryRouter>
                  </IndexationContextProvider>
                </AppStateContextProvider>
              </CurrentUserContextProvider>
            </AvailableFeaturesContext.Provider>
          </LanguagesContext.Provider>
        </MetricsContext.Provider>
      </IntlProvider>
    </HelmetProvider>
  );
}
