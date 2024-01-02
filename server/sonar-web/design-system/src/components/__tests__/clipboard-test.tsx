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
import { screen, waitForElementToBeRemoved } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithContext } from '../../helpers/testUtils';
import { ClipboardButton, ClipboardIconButton } from '../clipboard';

beforeEach(() => {
  jest.useFakeTimers();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

describe('ClipboardButton', () => {
  it('should display correctly', async () => {
    /* Delay: null is necessary to play well with fake timers
     * https://github.com/testing-library/user-event/issues/833
     */
    const user = userEvent.setup({ delay: null });
    renderClipboardButton();

    expect(screen.getByRole('button', { name: 'Copy' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Copy' }));

    expect(await screen.findByText('Copied')).toBeVisible();

    await waitForElementToBeRemoved(() => screen.queryByText('Copied'));
    jest.runAllTimers();
  });

  it('should render a custom label if provided', () => {
    renderClipboardButton('Foo Bar');
    expect(screen.getByRole('button', { name: 'Foo Bar' })).toBeInTheDocument();
  });

  function renderClipboardButton(children?: React.ReactNode) {
    renderWithContext(<ClipboardButton copyValue="foo">{children}</ClipboardButton>);
  }
});

describe('ClipboardIconButton', () => {
  it('should display correctly', () => {
    renderWithContext(<ClipboardIconButton copyValue="foo" />);

    const copyButton = screen.getByRole('button', { name: 'Copy to clipboard' });
    expect(copyButton).toBeInTheDocument();
  });
});
