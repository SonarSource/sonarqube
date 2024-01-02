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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Matcher, RenderResult, render, screen, within } from '@testing-library/react';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import { omit } from 'lodash';
import * as React from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { IntlProvider, ReactIntlErrorCode } from 'react-intl';
import {
  MemoryRouter,
  Outlet,
  Route,
  RouterProvider,
  Routes,
  createMemoryRouter,
  createRoutesFromElements,
  parsePath,
} from 'react-router-dom';
import AdminContext from '../app/components/AdminContext';
import GlobalMessagesContainer from '../app/components/GlobalMessagesContainer';
import AppStateContextProvider from '../app/components/app-state/AppStateContextProvider';
import { AvailableFeaturesContext } from '../app/components/available-features/AvailableFeaturesContext';
import { ComponentContext } from '../app/components/componentContext/ComponentContext';
import CurrentUserContextProvider from '../app/components/current-user/CurrentUserContextProvider';
import IndexationContextProvider from '../app/components/indexation/IndexationContextProvider';
import { LanguagesContext } from '../app/components/languages/LanguagesContext';
import { MetricsContext } from '../app/components/metrics/MetricsContext';
import { useLocation } from '../components/hoc/withRouter';
import { AppState } from '../types/appstate';
import { ComponentContextShape } from '../types/component';
import { Feature } from '../types/features';
import { Component, Dict, Extension, Languages, Metric, SysStatus } from '../types/types';
import { CurrentUser } from '../types/users';
import { mockComponent } from './mocks/component';
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
  overrides: { systemStatus?: SysStatus; adminPages?: Extension[] } = {},
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
    context,
  );
}

export function renderComponent(
  component: React.ReactElement,
  pathname = '/',
  { appState = mockAppState(), featureList = [] }: RenderContext = {},
) {
  function Wrapper({ children }: { children: React.ReactElement }) {
    const queryClient = new QueryClient();

    return (
      <IntlWrapper>
        <QueryClientProvider client={queryClient}>
          <HelmetProvider>
            <AvailableFeaturesContext.Provider value={featureList}>
              <AppStateContextProvider appState={appState}>
                <MemoryRouter initialEntries={[pathname]}>
                  <Routes>
                    <Route path="*" element={children} />
                  </Routes>
                </MemoryRouter>
              </AppStateContextProvider>
            </AvailableFeaturesContext.Provider>
          </HelmetProvider>
        </QueryClientProvider>
      </IntlWrapper>
    );
  }

  return render(component, { wrapper: Wrapper });
}

export function renderAppWithComponentContext(
  indexPath: string,
  routes: () => JSX.Element,
  context: RenderContext = {},
  componentContext: Partial<ComponentContextShape> = {},
) {
  function MockComponentContainer() {
    const [realComponent, setRealComponent] = React.useState(
      componentContext?.component ?? mockComponent(),
    );
    return (
      <ComponentContext.Provider
        value={{
          onComponentChange: (changes: Partial<Component>) => {
            setRealComponent({ ...realComponent, ...changes });
          },
          fetchComponent: jest.fn(),
          component: realComponent,
          ...omit(componentContext, 'component'),
        }}
      >
        <Outlet />
      </ComponentContext.Provider>
    );
  }

  return renderRoutedApp(
    <Route element={<MockComponentContainer />}>{routes()}</Route>,
    indexPath,
    context,
  );
}

export function renderApp(
  indexPath: string,
  component: JSX.Element,
  context: RenderContext = {},
): RenderResult {
  return renderRoutedApp(<Route path={indexPath} element={component} />, indexPath, context);
}

export function renderAppRoutes(
  indexPath: string,
  routes: () => JSX.Element,
  context?: RenderContext,
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
  }: RenderContext = {},
): RenderResult {
  const path = parsePath(navigateTo);
  if (!path.pathname?.startsWith('/')) {
    path.pathname = `/${path.pathname}`;
  }
  const queryClient = new QueryClient();

  const router = createMemoryRouter(
    createRoutesFromElements(
      <>
        {children}
        <Route path="*" element={<CatchAll />} />
      </>,
    ),
    { initialEntries: [path] },
  );

  return render(
    <HelmetProvider context={{}}>
      <IntlWrapper>
        <MetricsContext.Provider value={metrics}>
          <LanguagesContext.Provider value={languages}>
            <AvailableFeaturesContext.Provider value={featureList}>
              <CurrentUserContextProvider currentUser={currentUser}>
                <AppStateContextProvider appState={appState}>
                  <IndexationContextProvider>
                    <QueryClientProvider client={queryClient}>
                      <GlobalMessagesContainer />
                      <RouterProvider router={router} />
                    </QueryClientProvider>
                  </IndexationContextProvider>
                </AppStateContextProvider>
              </CurrentUserContextProvider>
            </AvailableFeaturesContext.Provider>
          </LanguagesContext.Provider>
        </MetricsContext.Provider>
      </IntlWrapper>
    </HelmetProvider>,
  );
}

export function dateInputEvent(user: UserEvent) {
  return {
    async pickDate(element: HTMLElement, date: Date) {
      await user.click(element);

      const formatter = new Intl.DateTimeFormat('en', { month: 'long' });

      await user.selectOptions(
        await screen.findByRole('combobox', { name: 'Month:' }),
        formatter.format(date),
      );
      await user.selectOptions(
        screen.getByRole('combobox', { name: 'Year:' }),
        String(date.getFullYear()),
      );

      await user.click(screen.getByRole('gridcell', { name: String(date.getDate()) }));
    },
  };
}
/* eslint-enable testing-library/no-node-access */

/**
 * @deprecated Use our custom toHaveATooltipWithContent() matcher instead.
 */
export function findTooltipWithContent(
  text: Matcher,
  target?: HTMLElement,
  selector = 'svg > desc',
) {
  // eslint-disable-next-line no-console
  console.warn(`The usage of findTooltipWithContent() is deprecated; use expect.toHaveATooltipWithContent() instead.
Example:
  await expect(node).toHaveATooltipWithContent('foo.bar');`);
  return target
    ? within(target).getByText(text, { selector })
    : screen.getByText(text, { selector });
}

export function IntlWrapper({
  children,
  messages = {},
}: {
  children: React.ReactNode;
  messages?: Record<string, string>;
}) {
  return (
    <IntlProvider
      defaultLocale="en"
      locale="en"
      messages={messages}
      onError={(e) => {
        // ignore missing translations, there are none!
        if (e.code !== ReactIntlErrorCode.MISSING_TRANSLATION) {
          // eslint-disable-next-line no-console
          console.error(e);
        }
      }}
    >
      {children}
    </IntlProvider>
  );
}
