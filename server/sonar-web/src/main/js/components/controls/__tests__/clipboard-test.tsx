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
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import {
  ClipboardBase,
  ClipboardButton,
  ClipboardButtonProps,
  ClipboardIconButton,
  ClipboardIconButtonProps,
} from '../clipboard';

beforeEach(() => {
  jest.useFakeTimers();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

describe('ClipboardBase', () => {
  it('should display correctly', () => {
    renderClipboardBase();
    expect(screen.getByText('click to copy')).toBeInTheDocument();
  });

  it('should allow its content to be copied', async () => {
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderClipboardBase();

    await user.click(screen.getByRole('button'));
    expect(await screen.findByText('copied')).toBeInTheDocument();

    act(() => jest.runAllTimers());

    expect(await screen.findByText('click to copy')).toBeInTheDocument();
  });

  function renderClipboardBase(props: Partial<ClipboardBase['props']> = {}) {
    return renderComponent(
      <ClipboardBase {...props}>
        {({ setCopyButton, copySuccess }) => (
          <span data-clipboard-text="foo" ref={setCopyButton} role="button">
            {copySuccess ? 'copied' : 'click to copy'}
          </span>
        )}
      </ClipboardBase>,
    );
  }
});

describe('ClipboardButton', () => {
  it('should display correctly', () => {
    renderClipboardButton();
    expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
  });

  it('should render a custom label if provided', () => {
    renderClipboardButton({ children: 'custom label' });
    expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
    expect(screen.getByText('custom label')).toBeInTheDocument();
  });

  it('should render a custom aria-label if provided', () => {
    renderClipboardButton({ 'aria-label': 'custom label' });
    expect(screen.getByRole('button', { name: 'custom label' })).toBeInTheDocument();
  });

  function renderClipboardButton(props: Partial<ClipboardButtonProps> = {}) {
    return renderComponent(<ClipboardButton copyValue="foo" {...props} />);
  }
});

describe('ClipboardIconButton', () => {
  it('should display correctly', () => {
    renderClipboardIconButton();
    expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();
  });

  it('should render a custom aria-label if provided', () => {
    renderClipboardIconButton({ 'aria-label': 'custom label' });
    expect(screen.getByRole('button', { name: 'custom label' })).toBeInTheDocument();
  });

  function renderClipboardIconButton(props: Partial<ClipboardIconButtonProps> = {}) {
    return renderComponent(<ClipboardIconButton copyValue="foo" {...props} />);
  }
});
