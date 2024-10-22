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
import { render, screen, waitFor } from '@testing-library/react';
import { HelmetProvider } from 'react-helmet-async';
import { IntlProvider } from 'react-intl';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import { getExtensionStart } from '../../../../helpers/extensions';
import { mockComponent } from '../../../../helpers/mocks/component';
import { ComponentContextShape } from '../../../../types/component';
import { Component } from '../../../../types/types';
import { ComponentContext } from '../../componentContext/ComponentContext';
import ProjectPageExtension, { ProjectPageExtensionProps } from '../ProjectPageExtension';

jest.mock('../../../../helpers/extensions', () => ({
  getExtensionStart: jest.fn().mockResolvedValue(jest.fn()),
}));

const handler = new BranchesServiceMock();

beforeEach(() => {
  handler.reset();
});

it('should not render when no component is passed', () => {
  renderProjectPageExtension();
  expect(screen.queryByText('page_not_found')).not.toBeInTheDocument();
  expect(getExtensionStart).not.toHaveBeenCalledWith('pluginId/extensionId');
});

it('should render correctly when the extension is found', async () => {
  renderProjectPageExtension(
    mockComponent({ extensions: [{ key: 'pluginId/extensionId', name: 'name' }] }),
    { params: { pluginKey: 'pluginId', extensionKey: 'extensionId' } },
  );
  await waitFor(() => expect(getExtensionStart).toHaveBeenCalledWith('pluginId/extensionId'));
});

it('should render correctly when the extension is not found', async () => {
  renderProjectPageExtension(
    mockComponent({ extensions: [{ key: 'pluginId/extensionId', name: 'name' }] }),
    { params: { pluginKey: 'not-found-plugin', extensionKey: 'not-found-extension' } },
  );
  expect(await screen.findByText('page_not_found')).toBeInTheDocument();
});

function renderProjectPageExtension(
  component?: Component,
  props?: Partial<ProjectPageExtensionProps>,
) {
  const queryClient = new QueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <HelmetProvider context={{}}>
        <IntlProvider defaultLocale="en" locale="en">
          <ComponentContext.Provider value={{ component } as ComponentContextShape}>
            <MemoryRouter initialEntries={[`/?id=${component?.key}`]}>
              <Routes>
                <Route
                  path="*"
                  element={
                    <ProjectPageExtension
                      params={{ extensionKey: 'extensionId', pluginKey: 'pluginId' }}
                      {...props}
                    />
                  }
                />
              </Routes>
            </MemoryRouter>
          </ComponentContext.Provider>
        </IntlProvider>
      </HelmetProvider>
    </QueryClientProvider>,
  );
}
