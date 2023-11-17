/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import WebApiServiceMock from '../../../api/mocks/WebApiServiceMock';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { byRole, byTestId, byText } from '../../../helpers/testSelector';
import WebApiApp from '../WebApiApp';

const handler = new WebApiServiceMock();

const ui = {
  search: byRole('searchbox'),
  title: byRole('link', { name: 'Swagger Petstore - OpenAPI 3.0 1.0.17' }),
  searchClear: byRole('button', { name: 'clear' }),
  apiScopePet: byRole('button', { name: 'pet' }),
  apiScopeStore: byRole('button', { name: 'store' }),
  apiScopeUser: byRole('button', { name: 'user' }),
  apiSidebarItem: byTestId('js-subnavigation-item'),
  requestBody: byText('api_documentation.v2.request_subheader.request_body'),
  queryParameter: byRole('list', { name: 'api_documentation.v2.request_subheader.query' }).byRole(
    'listitem',
  ),
  pathParameter: byRole('list', { name: 'api_documentation.v2.request_subheader.path' }).byRole(
    'listitem',
  ),
  requestHeader: byRole('list', { name: 'api_documentation.v2.request_subheader.header' }).byRole(
    'listitem',
  ),
  requestBodyParameter: byRole('list', {
    name: 'api_documentation.v2.request_subheader.request_body',
  }).byRole('listitem'),
  response: byRole('list', { name: 'api_documentation.v2.response_header' }).byRole('listitem'),
};

beforeEach(() => {
  handler.reset();
});

it('should search apis', async () => {
  const user = userEvent.setup();
  renderWebApiApp();
  expect(await ui.apiScopePet.find()).toBeInTheDocument();
  expect(ui.apiScopeStore.get()).toBeInTheDocument();
  expect(ui.apiScopeUser.get()).toBeInTheDocument();
  expect(ui.apiSidebarItem.queryAll()).toHaveLength(0);

  await user.click(ui.apiScopePet.get());
  expect(ui.apiSidebarItem.getAll()).toHaveLength(8);
  await user.click(ui.apiScopeStore.get());
  expect(ui.apiSidebarItem.getAll()).toHaveLength(12);
  await user.click(ui.apiScopeUser.get());
  expect(ui.apiSidebarItem.getAll()).toHaveLength(19);

  await user.type(ui.search.get(), 'put');
  const putItems = ui.apiSidebarItem.getAll();
  expect(putItems).toHaveLength(3);
  expect(ui.apiScopePet.get()).toBeInTheDocument();
  expect(ui.apiScopeStore.query()).not.toBeInTheDocument();
  expect(ui.apiScopeUser.get()).toBeInTheDocument();
  expect(putItems[0]).toHaveTextContent('PUTUpdate an existing pet');
  expect(putItems[1]).toHaveTextContent('POSTCreates list of users with given input array');

  await user.click(ui.searchClear.get());

  expect(ui.apiScopeStore.get()).toBeInTheDocument();
  expect(ui.apiSidebarItem.getAll().length).toBeGreaterThan(3);
});

it('should navigate between apis', async () => {
  const user = userEvent.setup();
  renderWebApiApp();
  await user.click(await ui.apiScopePet.find());

  await user.click(ui.apiSidebarItem.getAt(0));
  expect(await screen.findByText('/api/v3/pet')).toBeInTheDocument();
  expect(await screen.findByText('Update an existing pet by Id')).toBeInTheDocument();
  expect(ui.response.getAll()).toHaveLength(4);
  expect(ui.queryParameter.query()).not.toBeInTheDocument();
  expect(ui.pathParameter.query()).not.toBeInTheDocument();
  expect(ui.requestHeader.query()).not.toBeInTheDocument();
  expect(ui.requestBody.get()).toBeInTheDocument();
  expect(ui.requestBodyParameter.getAll()).toHaveLength(6);
  expect(ui.requestBodyParameter.byRole('button').getAt(0)).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  await user.click(ui.requestBodyParameter.byRole('button').getAt(0));
  expect(ui.requestBodyParameter.byRole('button').getAt(0)).toHaveAttribute(
    'aria-expanded',
    'true',
  );
  expect(ui.requestBodyParameter.getAt(0)).toHaveTextContent('name requiredmax: 100min: 3');
  await user.click(ui.requestBodyParameter.byRole('button').getAt(0));
  expect(ui.requestBodyParameter.byRole('button').getAt(0)).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  expect(ui.response.byRole('button').getAt(0)).toHaveAttribute('aria-expanded', 'true');
  expect(ui.response.byRole('button').getAt(2)).toHaveAttribute('aria-expanded', 'false');
  expect(ui.response.getAt(0)).toHaveTextContent('200Successful operation');
  expect(ui.response.getAt(0)).toHaveTextContent('"name": "string"');
  expect(ui.response.getAt(1)).not.toHaveTextContent('no_data');
  await user.click(ui.response.byRole('button').getAt(2));
  expect(ui.response.getAt(0)).toHaveTextContent('"name": "string"');
  expect(ui.response.getAt(1)).toHaveTextContent('no_data');
  await user.click(ui.response.byRole('button').getAt(0));
  expect(ui.response.getAt(0)).not.toHaveTextContent('"name": "string"');
  expect(ui.response.getAt(1)).toHaveTextContent('no_data');

  await user.click(ui.apiSidebarItem.getAt(2));
  expect(await screen.findByText('/api/v3/pet/findByStatus')).toBeInTheDocument();
  expect(ui.response.byRole('button').getAt(0)).toHaveAttribute('aria-expanded', 'true');
  expect(ui.response.byRole('button').getAt(2)).toHaveAttribute('aria-expanded', 'false');
  expect(ui.queryParameter.get()).toBeInTheDocument();
  expect(ui.pathParameter.query()).not.toBeInTheDocument();
  expect(ui.requestHeader.query()).not.toBeInTheDocument();
  expect(ui.requestBody.query()).not.toBeInTheDocument();
  expect(ui.queryParameter.getAll()).toHaveLength(1);
  expect(ui.queryParameter.getAt(0)).toHaveTextContent('status ["available","pending","sold"]');
  expect(ui.queryParameter.getAt(0)).not.toHaveTextContent('default: available');
  await user.click(ui.queryParameter.byRole('button').getAt(0));
  expect(ui.queryParameter.getAt(0)).toHaveTextContent('default: available');
  await user.click(ui.queryParameter.byRole('button').getAt(0));
  expect(ui.queryParameter.getAt(0)).not.toHaveTextContent('default: available');

  await user.click(ui.apiSidebarItem.getAt(5));
  expect(await screen.findByText('/api/v3/pet/{petId}')).toBeInTheDocument();
  expect(ui.queryParameter.getAll()).toHaveLength(2);
  expect(ui.pathParameter.getAll()).toHaveLength(1);
  expect(ui.requestHeader.query()).not.toBeInTheDocument();
  expect(ui.requestBody.query()).not.toBeInTheDocument();
  expect(ui.pathParameter.getAt(0)).toHaveTextContent('petId integer (int64)required');
  expect(ui.pathParameter.getAt(0)).not.toHaveTextContent('ID of pet that needs to be updated');
  await user.click(ui.pathParameter.byRole('button').getAt(0));
  expect(ui.pathParameter.getAt(0)).toHaveTextContent('ID of pet that needs to be updated');
  expect(ui.queryParameter.getAt(0)).toHaveTextContent('deprecated');
  expect(ui.queryParameter.getAt(1)).not.toHaveTextContent('deprecated');
  await user.click(ui.queryParameter.byRole('button').getAt(1));
  expect(ui.queryParameter.getAt(1)).toHaveTextContent('max: 5min: -1example: 3');

  await user.click(ui.apiSidebarItem.getAt(7));
  expect(await screen.findByText('/api/v3/pet/{petId}/uploadImage')).toBeInTheDocument();
  expect(screen.getByText('no_data')).toBeInTheDocument();
});

it('should show About page', async () => {
  const user = userEvent.setup();
  renderWebApiApp();
  expect(await screen.findByText('about')).toBeInTheDocument();
  expect(
    screen.getByText('This is a sample Pet Store Server based on the OpenAPI 3.0 specification.', {
      exact: false,
    }),
  ).toBeInTheDocument();
  await user.click(ui.apiScopePet.get());
  await user.click(ui.apiSidebarItem.getAt(0));
  expect(screen.queryByText('about')).not.toBeInTheDocument();
  await user.click(ui.title.get());
  expect(await screen.findByText('about')).toBeInTheDocument();
});

function renderWebApiApp() {
  // eslint-disable-next-line testing-library/no-unnecessary-act
  renderApp('web-api-v2', <WebApiApp />);
}
