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
import { render } from '../../helpers/testUtils';
import { BaseFacetItem, FacetItemProps } from '../FacetItem';

it('should render a disabled facet item', async () => {
  const user = userEvent.setup();

  const onClick = jest.fn();

  renderComponent({ disabled: true, onClick });

  expect(screen.getByRole('checkbox')).toHaveAttribute('aria-disabled', 'true');

  await user.click(screen.getByRole('checkbox'));

  expect(onClick).not.toHaveBeenCalled();
});

it('should render a non-disabled facet item', async () => {
  const user = userEvent.setup();

  const onClick = jest.fn();

  renderComponent({ active: true, onClick, stat: 3, value: 'foo' });

  expect(screen.getByRole('checkbox')).toHaveAttribute('aria-disabled', 'false');

  await user.click(screen.getByRole('checkbox'));

  expect(onClick).toHaveBeenCalledWith('foo', false);

  await user.keyboard('{Meta>}');
  await user.click(screen.getByRole('checkbox'));

  expect(onClick).toHaveBeenLastCalledWith('foo', true);
});

it('should add an aria label if the name is a string', () => {
  renderComponent({ name: 'Foo' });

  expect(screen.getByRole('checkbox')).toHaveAccessibleName('Foo');
});

it('should not add an aria label if the name is not a string', () => {
  renderComponent({ name: <div>Foo</div>, small: true });

  expect(screen.getByRole('checkbox')).not.toHaveAttribute('aria-label');
});

function renderComponent(props: Partial<FacetItemProps> = {}) {
  return render(
    <BaseFacetItem name="Test facet item" onClick={jest.fn()} value="Value" {...props} />,
  );
}
