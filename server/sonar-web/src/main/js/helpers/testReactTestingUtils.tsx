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
import { render, RenderResult } from '@testing-library/react';
import { History } from 'history';
import * as React from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { IntlProvider } from 'react-intl';
import { Provider } from 'react-redux';
import {
  createMemoryHistory,
  Route,
  RouteComponent,
  RouteConfig,
  Router,
  withRouter,
  WithRouterProps
} from 'react-router';
import { Store } from 'redux';
import AdminContext from '../app/components/AdminContext';
import AppStateContextProvider from '../app/components/app-state/AppStateContextProvider';
import CurrentUserContextProvider from '../app/components/current-user/CurrentUserContextProvider';
import { LanguagesContext } from '../app/components/languages/LanguagesContext';
import { MetricsContext } from '../app/components/metrics/MetricsContext';
import getStore from '../app/utils/getStore';
import { RouteWithChildRoutes } from '../app/utils/startReactApp';
import { Store as State } from '../store/rootReducer';
import { AppState } from '../types/appstate';
import { Dict, Extension, Languages, Metric, SysStatus } from '../types/types';
import { CurrentUser } from '../types/users';
import { DEFAULT_METRICS } from './mocks/metrics';
import { mockAppState, mockCurrentUser } from './testMocks';

interface RenderContext {
  metrics?: Dict<Metric>;
  store?: Store<State, any>;
  history?: History;
  appState?: AppState;
  languages?: Languages;
  currentUser?: CurrentUser;
  navigateTo?: string;
}

export function renderAdminApp(
  indexPath: string,
  routes: RouteConfig,
  context: RenderContext = {},
  overrides: { systemStatus?: SysStatus; adminPages?: Extension[] } = {}
): RenderResult {
  function MockAdminContainer(props: { children: React.ReactElement }) {
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
          systemStatus: overrides.systemStatus ?? 'UP'
        }}>
        {React.cloneElement(props.children, {
          adminPages: overrides.adminPages ?? []
        })}
      </AdminContext.Provider>
    );
  }

  const innerPath = indexPath.split('admin/').pop();

  return renderRoutedApp(
    <Route component={MockAdminContainer} path="admin">
      <RouteWithChildRoutes path={innerPath} childRoutes={routes} />
    </Route>,
    indexPath,
    context
  );
}

export function renderComponent(component: React.ReactElement) {
  function Wrapper({ children }: { children: React.ReactElement }) {
    return (
      <IntlProvider defaultLocale="en" locale="en">
        {children}
      </IntlProvider>
    );
  }

  return render(component, { wrapper: Wrapper });
}

export function renderComponentApp(
  indexPath: string,
  component: RouteComponent,
  context: RenderContext = {}
): RenderResult {
  return renderRoutedApp(<Route path={indexPath} component={component} />, indexPath, context);
}

export function renderApp(
  indexPath: string,
  routes: RouteConfig,
  context?: RenderContext
): RenderResult {
  return renderRoutedApp(
    <RouteWithChildRoutes path={indexPath} childRoutes={routes} />,
    indexPath,
    context
  );
}

const CatchAll = withRouter((props: WithRouterProps) => {
  return (
    <div>{`${props.location.pathname}?${new URLSearchParams(
      props.location.query
    ).toString()}`}</div>
  );
});

function renderRoutedApp(
  children: React.ReactElement,
  indexPath: string,
  {
    currentUser = mockCurrentUser(),
    navigateTo = indexPath,
    metrics = DEFAULT_METRICS,
    store = getStore(),
    appState = mockAppState(),
    history = createMemoryHistory(),
    languages = {}
  }: RenderContext = {}
): RenderResult {
  history.push(`/${navigateTo}`);
  return render(
    <HelmetProvider context={{}}>
      <IntlProvider defaultLocale="en" locale="en">
        <MetricsContext.Provider value={metrics}>
          <Provider store={store}>
            <LanguagesContext.Provider value={languages}>
              <CurrentUserContextProvider currentUser={currentUser}>
                <AppStateContextProvider appState={appState}>
                  <Router history={history}>
                    {children}
                    <Route path="*" component={CatchAll} />
                  </Router>
                </AppStateContextProvider>
              </CurrentUserContextProvider>
            </LanguagesContext.Provider>
          </Provider>
        </MetricsContext.Provider>
      </IntlProvider>
    </HelmetProvider>
  );
}
