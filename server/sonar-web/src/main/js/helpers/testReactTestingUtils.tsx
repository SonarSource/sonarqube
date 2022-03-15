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
import { createMemoryHistory, Route, RouteComponent, RouteConfig, Router } from 'react-router';
import { Store } from 'redux';
import AppStateContextProvider from '../app/components/app-state/AppStateContextProvider';
import CurrentUserContextProvider from '../app/components/current-user/CurrentUserContextProvider';
import { MetricsContext } from '../app/components/metrics/MetricsContext';
import getStore from '../app/utils/getStore';
import { RouteWithChildRoutes } from '../app/utils/startReactApp';
import { Store as State } from '../store/rootReducer';
import { AppState } from '../types/appstate';
import { Dict, Metric } from '../types/types';
import { CurrentUser } from '../types/users';
import { DEFAULT_METRICS } from './mocks/metrics';
import { mockAppState, mockCurrentUser } from './testMocks';

interface RenderContext {
  metrics?: Dict<Metric>;
  store?: Store<State, any>;
  history?: History;
  appState?: AppState;
  currentUser?: CurrentUser;
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
  context: RenderContext
): RenderResult {
  return renderRoutedApp(
    <RouteWithChildRoutes path={indexPath} childRoutes={routes} />,
    indexPath,
    context
  );
}

function renderRoutedApp(
  children: React.ReactElement,
  indexPath: string,
  {
    currentUser = mockCurrentUser(),
    metrics = DEFAULT_METRICS,
    store = getStore(),
    appState = mockAppState(),
    history = createMemoryHistory()
  }: RenderContext = {}
): RenderResult {
  history.push(`/${indexPath}`);
  return render(
    <HelmetProvider context={{}}>
      <IntlProvider defaultLocale="en" locale="en">
        <MetricsContext.Provider value={metrics}>
          <Provider store={store}>
            <CurrentUserContextProvider currentUser={currentUser}>
              <AppStateContextProvider appState={appState}>
                <Router history={history}>{children}</Router>
              </AppStateContextProvider>
            </CurrentUserContextProvider>
          </Provider>
        </MetricsContext.Provider>
      </IntlProvider>
    </HelmetProvider>
  );
}
