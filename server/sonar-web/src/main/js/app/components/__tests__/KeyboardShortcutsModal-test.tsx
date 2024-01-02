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
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { byRole } from '../../../helpers/testSelector';
import KeyboardShortcutsModal from '../KeyboardShortcutsModal';

it('should render correctly', async () => {
  const user = userEvent.setup();
  renderKeyboardShortcutsModal();

  expect(ui.modalTitle.query()).not.toBeInTheDocument();

  await act(() => user.keyboard('?'));

  expect(ui.modalTitle.get()).toBeInTheDocument();

  await user.click(ui.closeButton.get());

  expect(ui.modalTitle.query()).not.toBeInTheDocument();
});

it('should ignore other keydownes', async () => {
  const user = userEvent.setup();
  renderKeyboardShortcutsModal();

  await act(() => user.keyboard('!'));

  expect(ui.modalTitle.query()).not.toBeInTheDocument();
});

it('should ignore events in an input', async () => {
  const user = userEvent.setup();

  renderKeyboardShortcutsModal();

  await user.click(ui.textInput.get());
  await act(() => user.keyboard('?'));

  expect(ui.modalTitle.query()).not.toBeInTheDocument();
});

function renderKeyboardShortcutsModal() {
  return renderComponent(
    <>
      <KeyboardShortcutsModal />
      <input type="text" />
    </>,
  );
}

const ui = {
  modalTitle: byRole('heading', { name: 'keyboard_shortcuts.title' }),
  closeButton: byRole('button', { name: 'close' }),

  textInput: byRole('textbox'),
};
