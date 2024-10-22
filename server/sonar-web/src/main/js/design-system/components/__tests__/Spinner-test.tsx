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
import { IntlWrapper } from '../../helpers/testUtils';
import { Spinner } from '../Spinner';

it('allows setting a custom class name', () => {
  renderSpinner({ className: 'foo' });
  expect(screen.getByRole('status')).toHaveClass('foo');
});

it('can be controlled by the loading prop', () => {
  const { rerender } = renderSpinner({ loading: true });
  expect(screen.getByText('loading')).toBeInTheDocument();

  rerender(prepareSpinner({ loading: false }));
  expect(screen.queryByText('loading')).not.toBeInTheDocument();
});

function renderSpinner(props: Partial<Parameters<typeof Spinner>[0]> = {}) {
  // We don't use our renderComponent() helper here, as we have some tests that
  // require changes in props.
  return render(prepareSpinner(props));
}

function prepareSpinner(props: Partial<Parameters<typeof Spinner>[0]> = {}) {
  return (
    <IntlWrapper>
      <Spinner {...props} />
    </IntlWrapper>
  );
}
