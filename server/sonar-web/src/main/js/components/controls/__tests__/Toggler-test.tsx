/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { act, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup';
import * as React from 'react';
import { byRole } from 'testing-library-selector';
import Toggler from '../Toggler';

beforeAll(() => {
  jest.useFakeTimers();
});

afterAll(() => {
  jest.useRealTimers();
});
const ui = {
  toggleButton: byRole('button', { name: 'toggle' }),
  outButton: byRole('button', { name: 'out' }),
  overlayButton: byRole('button', { name: 'overlay' }),
};

async function openToggler(user: UserEvent) {
  await user.click(ui.toggleButton.get());
  act(() => {
    jest.runAllTimers();
  });
  expect(ui.overlayButton.get()).toBeInTheDocument();
}

function focusOut() {
  act(() => {
    ui.overlayButton.get().focus();
    ui.outButton.get().focus();
  });
}

it('should handle escape correclty', async () => {
  const user = userEvent.setup({ delay: null });
  const rerender = renderToggler({
    closeOnEscape: true,
    closeOnClick: false,
    closeOnClickOutside: false,
    closeOnFocusOut: false,
  });

  await openToggler(user);

  await user.keyboard('{Escape}');
  expect(ui.overlayButton.query()).not.toBeInTheDocument();

  rerender({ closeOnEscape: false });
  await openToggler(user);

  await user.keyboard('{Escape}');
  expect(ui.overlayButton.get()).toBeInTheDocument();
});

it('should handle focus correctly', async () => {
  const user = userEvent.setup({ delay: null });
  const rerender = renderToggler({
    closeOnEscape: false,
    closeOnClick: false,
    closeOnClickOutside: false,
    closeOnFocusOut: true,
  });

  await openToggler(user);

  focusOut();
  expect(ui.overlayButton.query()).not.toBeInTheDocument();

  rerender({ closeOnFocusOut: false });
  await openToggler(user);

  focusOut();
  expect(ui.overlayButton.get()).toBeInTheDocument();
});

it('should handle click correctly', async () => {
  const user = userEvent.setup({ delay: null });
  const rerender = renderToggler({
    closeOnEscape: false,
    closeOnClick: true,
    closeOnClickOutside: false,
    closeOnFocusOut: false,
  });

  await openToggler(user);

  await user.click(ui.outButton.get());
  expect(ui.overlayButton.query()).not.toBeInTheDocument();

  await openToggler(user);

  await user.click(ui.overlayButton.get());
  expect(ui.overlayButton.query()).not.toBeInTheDocument();

  rerender({ closeOnClick: false });
  await openToggler(user);

  await user.click(ui.outButton.get());
  expect(ui.overlayButton.get()).toBeInTheDocument();
});

it('should handle click outside correctly', async () => {
  const user = userEvent.setup({ delay: null });
  const rerender = renderToggler({
    closeOnEscape: false,
    closeOnClick: false,
    closeOnClickOutside: true,
    closeOnFocusOut: false,
  });

  await openToggler(user);

  await user.click(ui.overlayButton.get());
  expect(await ui.overlayButton.find()).toBeInTheDocument();

  await user.click(ui.outButton.get());
  expect(ui.overlayButton.query()).not.toBeInTheDocument();

  rerender({ closeOnClickOutside: false });
  await openToggler(user);

  await user.click(ui.outButton.get());
  expect(ui.overlayButton.get()).toBeInTheDocument();
});

it('should open/close correctly when default props is applied', async () => {
  const user = userEvent.setup({ delay: null });
  renderToggler();

  await openToggler(user);

  // Should not close when on overlay
  await user.click(ui.overlayButton.get());
  expect(await ui.overlayButton.find()).toBeInTheDocument();

  // Focus out should close
  act(() => {
    ui.overlayButton.get().focus();
    ui.outButton.get().focus();
  });
  expect(ui.overlayButton.query()).not.toBeInTheDocument();

  await openToggler(user);

  // Escape should close
  await user.keyboard('{Escape}');
  expect(ui.overlayButton.query()).not.toBeInTheDocument();

  await openToggler(user);

  // Click should close (focus out is trigger first)
  await user.click(ui.outButton.get());
  expect(ui.overlayButton.query()).not.toBeInTheDocument();
});

function renderToggler(override?: Partial<Toggler['props']>) {
  function App(props: Partial<Toggler['props']>) {
    const [open, setOpen] = React.useState(false);

    return (
      <>
        <Toggler
          onRequestClose={() => setOpen(false)}
          open={open}
          overlay={<button type="button">overlay</button>}
          {...props}
        >
          <button onClick={() => setOpen(true)} type="button">
            toggle
          </button>
        </Toggler>
        <button type="button">out</button>
      </>
    );
  }

  const { rerender } = render(<App {...override} />);
  return function (reoverride: Partial<Toggler['props']>) {
    return rerender(<App {...override} {...reoverride} />);
  };
}
