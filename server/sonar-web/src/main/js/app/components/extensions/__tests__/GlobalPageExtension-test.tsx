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

import { screen } from '@testing-library/react';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { Extension } from '../../../../types/types';
import GlobalPageExtension, { GlobalPageExtensionProps } from '../GlobalPageExtension';

jest.mock('../Extension', () => ({
  __esModule: true,
  default(props: { extension: { key: string; name: string } }) {
    return <h1>{props.extension.name}</h1>;
  },
}));

const extensions = [{ key: 'plugin123/ext42', name: 'extension 42' }];

it('should find the extension from params', () => {
  renderGlobalPageExtension('extension/plugin123/ext42', extensions);

  expect(screen.getByText('extension 42')).toBeInTheDocument();
});

it('should notify if extension is not found', () => {
  renderGlobalPageExtension('extension/plugin123/wrong-extension', extensions);

  expect(screen.getByText('page_not_found')).toBeInTheDocument();
});

it('should find the extension from props', () => {
  const params = { pluginKey: 'plugin123', extensionKey: 'ext42' };

  renderGlobalPageExtension('extension/whatever/overridden', extensions, params);

  expect(screen.getByText('extension 42')).toBeInTheDocument();
});

function renderGlobalPageExtension(
  navigateTo: string,
  globalPages: Extension[] = [],
  params?: GlobalPageExtensionProps['params'],
) {
  renderApp(`extension/:pluginKey/:extensionKey`, <GlobalPageExtension params={params} />, {
    appState: mockAppState({ globalPages }),
    navigateTo,
  });
}
