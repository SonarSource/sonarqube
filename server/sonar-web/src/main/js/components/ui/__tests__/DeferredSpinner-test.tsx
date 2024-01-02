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
import { render, screen } from '@testing-library/react';
import * as React from 'react';
import DeferredSpinner from '../DeferredSpinner';

beforeAll(() => {
  jest.useFakeTimers();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
});

afterAll(() => {
  jest.useRealTimers();
});

it('renders children before timeout', () => {
  renderDeferredSpinner({ children: <a href="#">foo</a> });
  expect(screen.getByRole('link')).toBeInTheDocument();
  jest.runAllTimers();
  expect(screen.queryByRole('link')).not.toBeInTheDocument();
});

it('renders spinner after timeout', () => {
  renderDeferredSpinner();
  expect(screen.queryByText('loading')).not.toBeInTheDocument();
  jest.runAllTimers();
  expect(screen.getByText('loading')).toBeInTheDocument();
});

it('allows setting a custom class name', () => {
  renderDeferredSpinner({ className: 'foo' });
  jest.runAllTimers();
  expect(screen.getByTestId('deferred-spinner')).toHaveClass('foo');
});

it('can be controlled by the loading prop', () => {
  const { rerender } = renderDeferredSpinner({ loading: true });
  jest.runAllTimers();
  expect(screen.getByText('loading')).toBeInTheDocument();

  rerender(prepareDeferredSpinner({ loading: false }));
  expect(screen.queryByText('loading')).not.toBeInTheDocument();
});

function renderDeferredSpinner(props: Partial<DeferredSpinner['props']> = {}) {
  // We don't use our renderComponent() helper here, as we have some tests that
  // require changes in props.
  return render(prepareDeferredSpinner(props));
}

function prepareDeferredSpinner(props: Partial<DeferredSpinner['props']> = {}) {
  return <DeferredSpinner ariaLabel="loading" {...props} />;
}
