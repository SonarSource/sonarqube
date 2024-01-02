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
import * as React from 'react';
import { Route } from 'react-router-dom';
import { mockAppState } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import DocumentationRedirect from '../DocumentationRedirect';

it('should redirect to static doc for specific version', async () => {
  renderDocumentationRedirect('land', '9.7.1234');

  expect(await screen.findByRole('link')).toHaveAttribute(
    'href',
    'https://docs.sonarqube.org/9.7/land'
  );
});

it('should redirect to static doc for latest version', async () => {
  renderDocumentationRedirect('land', '9.7-SNAPSHOT');

  expect(await screen.findByRole('link')).toHaveAttribute(
    'href',
    'https://docs.sonarqube.org/latest/land'
  );
});

function renderDocumentationRedirect(navigatge: string, version?: string) {
  renderAppRoutes(
    `documentation/${navigatge}`,
    () => <Route path="/documentation/*" element={<DocumentationRedirect />} />,
    { appState: mockAppState({ version }) }
  );
}
