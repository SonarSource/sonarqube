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
import { renderWithContext } from '../../helpers/testUtils';
import { FacetBox, FacetBoxProps } from '../FacetBox';

it('should render an empty disabled facet box', async () => {
  const user = userEvent.setup();

  const onClick = jest.fn();

  renderComponent({ disabled: true, hasEmbeddedFacets: true, onClick });

  expect(screen.queryByRole('group')).not.toBeInTheDocument();

  expect(screen.getByText('Test FacetBox')).toBeInTheDocument();

  expect(screen.getByRole('button', { expanded: false })).toHaveAttribute('aria-disabled', 'true');

  await user.click(screen.getByRole('button'));

  expect(onClick).not.toHaveBeenCalled();
});

it('should render an inner expanded facet box with count', async () => {
  const user = userEvent.setup();

  const onClear = jest.fn();
  const onClick = jest.fn();

  renderComponent({
    children: 'The panel',
    count: 3,
    inner: true,
    onClear,
    onClick,
    open: true,
  });

  expect(screen.getByRole('group')).toBeInTheDocument();

  expect(screen.getByRole('button', { expanded: true })).toBeInTheDocument();

  await user.click(screen.getByRole('button', { expanded: true }));

  expect(onClick).toHaveBeenCalledWith(false);
});

function renderComponent({ children, ...props }: Partial<FacetBoxProps> = {}) {
  return renderWithContext(
    <FacetBox name="Test FacetBox" {...props}>
      {children}
    </FacetBox>,
  );
}
