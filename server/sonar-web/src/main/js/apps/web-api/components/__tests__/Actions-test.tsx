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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { mockAction, mockDomain, mockParam } from '../../../../helpers/mocks/webapi';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import Action from '../Action';

jest.mock('../../../../api/web-api', () => ({
  fetchResponseExample: jest.fn().mockResolvedValue({
    example: '{"example": "response"}',
    format: 'json',
  }),
}));

it('should have no additional information links', () => {
  renderAction();

  expect(ui.changelogTab.query()).not.toBeInTheDocument();
  expect(ui.paramsTab.query()).not.toBeInTheDocument();
  expect(ui.responseExampleTab.query()).not.toBeInTheDocument();
});

it('should allow to browse additional information', async () => {
  const user = userEvent.setup();

  renderAction({
    action: mockAction({
      changelog: [
        { description: 'change: Thing added', version: '1.2' },
        { description: 'change: thing removed', version: '2.6' },
      ],
      since: '1.0',
      params: [
        mockParam({ key: 'id', description: 'param: identifier', required: true }),
        mockParam({ key: '2', description: 'param: other' }),
      ],
      hasResponseExample: true,
    }),
  });

  expect(ui.changelogTab.get()).toBeInTheDocument();
  expect(ui.paramsTab.get()).toBeInTheDocument();
  expect(ui.responseExampleTab.get()).toBeInTheDocument();

  // All tabs should be hidden
  expect(byText(/change:/).queryAll()).toHaveLength(0);
  expect(byRole('row', { name: /param:/ }).queryAll()).toHaveLength(0);
  expect(byText('{"example": "response"}').query()).not.toBeInTheDocument();

  await user.click(ui.changelogTab.get());

  expect(byText(/change:/).getAll()).toHaveLength(2);
  expect(byRole('row', { name: /param:/ }).queryAll()).toHaveLength(0);
  expect(byText('{"example": "response"}').query()).not.toBeInTheDocument();

  await user.click(ui.paramsTab.get());

  expect(byText(/change:/).queryAll()).toHaveLength(0);
  expect(byRole('row', { name: /param:/ }).getAll()).toHaveLength(2);
  expect(byText('{"example": "response"}').query()).not.toBeInTheDocument();

  await user.click(ui.responseExampleTab.get());

  expect(byText(/change:/).queryAll()).toHaveLength(0);
  expect(byRole('row', { name: /param:/ }).queryAll()).toHaveLength(0);
  expect(await byText('{"example": "response"}').find()).toBeInTheDocument();
});

function renderAction(props: Partial<React.ComponentProps<typeof Action>> = {}) {
  renderComponent(
    <Action
      action={mockAction()}
      domain={mockDomain()}
      showDeprecated={false}
      showInternal={false}
      {...props}
    />,
  );
}

const ui = {
  paramsTab: byRole('tab', { name: 'api_documentation.parameters' }),
  responseExampleTab: byRole('tab', { name: 'api_documentation.response_example' }),
  changelogTab: byRole('tab', { name: 'api_documentation.changelog' }),
};
