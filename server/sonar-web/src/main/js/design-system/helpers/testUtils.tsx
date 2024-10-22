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

import { RenderOptions, RenderResult, render as rtlRender } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { Options as UserEventsOptions } from '@testing-library/user-event/dist/types/options';
import { InitialEntry } from 'history';
import { identity, kebabCase } from 'lodash';
import React, { PropsWithChildren, ReactNode } from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { IntlProvider, ReactIntlErrorCode } from 'react-intl';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

type RenderResultWithUser = RenderResult & { user: UserEvent };

export function render(
  ui: React.ReactElement,
  options?: RenderOptions,
  userEventOptions?: UserEventsOptions,
): RenderResultWithUser {
  return { ...rtlRender(ui, options), user: userEvent.setup(userEventOptions) };
}

type RenderContextOptions = Omit<RenderOptions, 'wrapper'> & {
  initialEntries?: InitialEntry[];
  userEventOptions?: UserEventsOptions;
};

export function renderWithContext(
  ui: React.ReactElement,
  { userEventOptions, ...options }: RenderContextOptions = {},
) {
  return render(ui, { ...options, wrapper: getContextWrapper() }, userEventOptions);
}

interface RenderRouterOptions {
  additionalRoutes?: ReactNode;
}

export function renderWithRouter(
  ui: React.ReactElement,
  options: RenderContextOptions & RenderRouterOptions = {},
) {
  const { additionalRoutes, userEventOptions, ...renderOptions } = options;

  function RouterWrapper({ children }: React.PropsWithChildren<object>) {
    return (
      <HelmetProvider>
        <IntlWrapper>
          <MemoryRouter>
            <Routes>
              <Route element={children} path="/" />
              {additionalRoutes}
            </Routes>
          </MemoryRouter>
        </IntlWrapper>
      </HelmetProvider>
    );
  }

  return render(ui, { ...renderOptions, wrapper: RouterWrapper }, userEventOptions);
}

function getContextWrapper() {
  return function ContextWrapper({ children }: React.PropsWithChildren<object>) {
    return (
      <HelmetProvider>
        <IntlWrapper>{children}</IntlWrapper>
      </HelmetProvider>
    );
  };
}

export function mockComponent(name: string, transformProps: (props: any) => any = identity) {
  function MockedComponent({ ...props }: PropsWithChildren<any>) {
    return React.createElement('mocked-' + kebabCase(name), transformProps(props));
  }

  MockedComponent.displayName = `mocked(${name})`;
  return MockedComponent;
}

export const debounceTimer = jest
  .fn()
  .mockImplementation((callback: (...args: unknown[]) => void, timeout: number) => {
    let timeoutId: number;

    const debounced = jest.fn((...args: unknown[]) => {
      window.clearTimeout(timeoutId);

      timeoutId = window.setTimeout(() => {
        callback(...args);
      }, timeout);
    });

    (debounced as typeof debounced & { cancel: () => void }).cancel = jest.fn(() => {
      window.clearTimeout(timeoutId);
    });

    return debounced;
  });

export function IntlWrapper({
  children,
  messages = {},
}: {
  children: ReactNode;
  messages?: Record<string, string>;
}) {
  return (
    <IntlProvider
      defaultLocale="en"
      locale="en"
      messages={messages}
      onError={(e) => {
        // ignore missing translations, there are none!
        if (
          e.code !== ReactIntlErrorCode.MISSING_TRANSLATION &&
          e.code !== ReactIntlErrorCode.UNSUPPORTED_FORMATTER
        ) {
          // eslint-disable-next-line no-console
          console.error(e);
        }
      }}
    >
      {children}
    </IntlProvider>
  );
}
