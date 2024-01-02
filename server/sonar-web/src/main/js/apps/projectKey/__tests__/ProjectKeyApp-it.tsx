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
import { within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { last } from 'lodash';
import React from 'react';
import { act } from 'react-dom/test-utils';
import { Route } from 'react-router-dom';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { byRole } from '../../../helpers/testSelector';
import ProjectKeyApp from '../ProjectKeyApp';

const componentsMock = new ComponentsServiceMock();

afterEach(() => {
  componentsMock.reset();
});

it('can update project key', async () => {
  const { ui, user } = getPageObjects();
  const oldKey = componentsMock.components[0].component.key;
  const newKey = 'NEW_KEY';
  renderProjectKeyApp();

  // Renders
  expect(await ui.pageTitle.find()).toBeInTheDocument();

  // Can type and reset to the old key value
  expect(ui.newKeyInput.get()).toHaveValue(oldKey);
  await user.clear(ui.newKeyInput.get());
  await user.type(ui.newKeyInput.get(), newKey);
  expect(ui.resetInputButton.get()).toBeEnabled();
  await user.click(ui.resetInputButton.get());
  expect(ui.newKeyInput.get()).toHaveValue(oldKey);

  // Can update value
  await user.clear(ui.newKeyInput.get());
  await user.type(ui.newKeyInput.get(), newKey);
  await user.click(ui.updateInputButton.get());
  // Dialog should show old and new keys
  expect(within(ui.updateKeyDialog.get()).getByText(oldKey)).toBeInTheDocument();
  expect(within(ui.updateKeyDialog.get()).getByText(newKey)).toBeInTheDocument();
  await act(async () => {
    await user.click(last(ui.updateInputButton.getAll()) as HTMLElement);
  });
  expect(ui.updateInputButton.get()).toBeDisabled();

  expect(ui.newKeyInput.get()).toHaveValue(newKey);
});

function renderProjectKeyApp() {
  return renderAppWithComponentContext(
    'project/key',
    () => <Route path="project/key" element={<ProjectKeyApp />} />,
    {},
    { component: componentsMock.components[0].component },
  );
}

function getPageObjects() {
  const user = userEvent.setup();

  const ui = {
    pageTitle: byRole('heading', { name: 'update_key.page' }),
    updateKeyDialog: byRole('dialog'),
    newKeyInput: byRole('textbox'),
    updateInputButton: byRole('button', { name: 'update_verb' }),
    resetInputButton: byRole('button', { name: 'reset_verb' }),
  };

  return {
    ui,
    user,
  };
}
