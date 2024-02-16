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

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { addGlobalErrorMessage, lightTheme } from 'design-system';
import * as React from 'react';
import { IntlShape } from 'react-intl';
import { getEnhancedWindow } from '../../../../helpers/browser';
import { installExtensionsHandler } from '../../../../helpers/extensionsHandler';
import {
  mockAppState,
  mockCurrentUser,
  mockLocation,
  mockRouter,
} from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ExtensionStartMethodParameter } from '../../../../types/extension';
import Extension, { ExtensionProps } from '../Extension';

jest.mock('design-system', () => ({
  ...jest.requireActual('design-system'),
  addGlobalErrorMessage: jest.fn(),
}));

beforeAll(() => {
  installExtensionsHandler();

  getEnhancedWindow().registerExtension(
    'first-react-extension',
    ({ location, router }: ExtensionStartMethodParameter) => {
      const suffix = location.pathname === '/new' ? ' change' : '';
      const handleClick = () => {
        router.push('new');
      };
      return (
        <div>
          <button onClick={handleClick} type="button">
            Click first react{suffix}
          </button>
        </div>
      );
    },
  );

  getEnhancedWindow().registerExtension(
    'not-react-extension',
    ({ el }: ExtensionStartMethodParameter) => {
      if (el) {
        el.innerHTML = '<button type="button">Click not react</button>';
        return () => {
          el.innerHTML = '';
        };
      }
    },
  );

  getEnhancedWindow().registerExtension('second-extension', () => {
    return (
      <div>
        <button type="button">Click second</button>
      </div>
    );
  });
});

it('should render React extensions correctly', async () => {
  const user = userEvent.setup();
  renderExtention();

  expect(await screen.findByRole('button', { name: 'Click first react' })).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'Click first react' }));

  expect(
    await screen.findByRole('button', { name: 'Click first react change' }),
  ).toBeInTheDocument();
});

it('should unmount an extension before starting a new one', async () => {
  const rerender = renderExtention();
  await screen.findByRole('button', { name: 'Click first react' });

  rerender({ extension: { key: 'not-react-extension', name: 'not-react-extension' } });
  expect(await screen.findByRole('button', { name: 'Click not react' })).toBeInTheDocument();

  rerender({ extension: { key: 'second-extension', name: 'second-extension' } });
  expect(await screen.findByRole('button', { name: 'Click second' })).toBeInTheDocument();
});

it('should warn when no extension found', async () => {
  renderExtention({ extension: { key: 'unknown', name: 'null' } });

  // JSDOM is not handling script loading so we need to simulate that.
  await waitFor(() => {
    // eslint-disable-next-line testing-library/no-node-access
    const script = document.querySelector('script');
    expect(script).toBeInTheDocument();
    script!.onload!(new Event(''));
  });

  await new Promise(setImmediate);
  expect(addGlobalErrorMessage).toHaveBeenCalled();
});

function renderExtention(props: Partial<ExtensionProps> = {}) {
  const originalProp = {
    theme: lightTheme,
    appState: mockAppState(),
    currentUser: mockCurrentUser(),
    extension: { key: 'first-react-extension', name: 'first-react-extension' },
    intl: {} as IntlShape,
    location: mockLocation(),
    router: mockRouter(),
    updateCurrentUserHomepage: jest.fn(),
  } as const;
  const { rerender } = renderComponent(<Extension {...originalProp} {...props} />);

  return (rerenderProp: Partial<ExtensionProps> = {}) => {
    rerender(<Extension {...originalProp} {...props} {...rerenderProp} />);
  };
}
