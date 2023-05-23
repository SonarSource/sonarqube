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
import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import useScrollDownCompress from '../useScrollDownCompress';

beforeEach(() => {
  Object.defineProperties(window.document.documentElement, {
    clientHeight: { value: 500, configurable: true },
    scrollHeight: { value: 1000, configurable: true },
    scrollTop: { value: 0, configurable: true },
  });
});

it('set isScrolled and isCompressed to true when scrolling down', async () => {
  renderComponent(<X />);

  expect(screen.getByText('isScrolled: false')).toBeVisible();
  expect(screen.getByText('isCompressed: false')).toBeVisible();

  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 200, configurable: true },
  });
  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: false')).toBeVisible();

  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 250, configurable: true },
  });
  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: true')).toBeVisible();

  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 260, configurable: true },
    scrollHeight: { value: 800, configurable: true },
  });
  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: true')).toBeVisible();

  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 5, configurable: true },
  });
  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: false')).toBeVisible();

  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 5, configurable: true },
    scrollHeight: { value: 1000, configurable: true },
  });
  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: false')).toBeVisible();
  expect(await screen.findByText('isCompressed: false')).toBeVisible();
});

it('reset the scroll state', async () => {
  const user = userEvent.setup();
  renderComponent(<X />);

  expect(screen.getByText('isScrolled: false')).toBeVisible();
  expect(screen.getByText('isCompressed: false')).toBeVisible();

  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 200, configurable: true },
  });
  fireEvent.scroll(window.document);
  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 250, configurable: true },
  });
  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: true')).toBeVisible();

  await user.click(screen.getByText('reset Compress'));
  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 300, configurable: true },
  });
  await fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: false')).toBeVisible();
});

it('keep the compressed state if scroll dont move', async () => {
  renderComponent(<X />);

  expect(screen.getByText('isScrolled: false')).toBeVisible();
  expect(screen.getByText('isCompressed: false')).toBeVisible();

  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 200, configurable: true },
  });
  fireEvent.scroll(window.document);
  Object.defineProperties(window.document.documentElement, {
    scrollTop: { value: 250, configurable: true },
  });
  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: true')).toBeVisible();

  fireEvent.scroll(window.document);
  expect(await screen.findByText('isScrolled: true')).toBeVisible();
  expect(await screen.findByText('isCompressed: true')).toBeVisible();
});

function X() {
  const { isScrolled, isCompressed, resetScrollDownCompress } = useScrollDownCompress(100, 10);

  return (
    <div>
      <div>isScrolled: {`${isScrolled}`}</div>
      <div>isCompressed: {`${isCompressed}`}</div>
      <button onClick={resetScrollDownCompress} type="button">
        reset Compress
      </button>
    </div>
  );
}
