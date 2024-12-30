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

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ListStyleFacetFooter, Props } from '../ListStyleFacetFooter';

it('should render "show more", not "show less"', async () => {
  const showMore = jest.fn();

  render({
    nbShown: 7,
    showLessAriaLabel: 'show less',
    showMore,
    showMoreAriaLabel: 'show more',
    total: 42,
  });

  expect(screen.getByText('x_show.7')).toBeInTheDocument();
  expect(screen.getByText('show_more')).toBeInTheDocument();
  expect(screen.getByLabelText('show more')).toBeInTheDocument();
  expect(screen.queryByText('show_less')).not.toBeInTheDocument();
  expect(screen.queryByLabelText('show less')).not.toBeInTheDocument();

  await userEvent.click(screen.getByLabelText('show more'));

  expect(showMore).toHaveBeenCalled();
});

it('should render neither "show more" nor "show less"', () => {
  render({ nbShown: 42, total: 42 });

  expect(screen.getByText('x_show.42')).toBeInTheDocument();
  expect(screen.queryByText('show_more')).not.toBeInTheDocument();
  expect(screen.queryByText('show_less')).not.toBeInTheDocument();
});

it('should render "show less", not "show more"', async () => {
  const showLess = jest.fn();

  render({
    nbShown: 42,
    showLess,
    showLessAriaLabel: 'show less',
    showMoreAriaLabel: 'show more',
    total: 42,
  });

  expect(screen.getByText('x_show.42')).toBeInTheDocument();
  expect(screen.queryByText('show_more')).not.toBeInTheDocument();
  expect(screen.queryByLabelText('show more')).not.toBeInTheDocument();
  expect(screen.getByText('show_less')).toBeInTheDocument();
  expect(screen.getByLabelText('show less')).toBeInTheDocument();

  await userEvent.click(screen.getByLabelText('show less'));

  expect(showLess).toHaveBeenCalled();
});

function render(props: Partial<Props> = {}) {
  return renderComponent(
    <ListStyleFacetFooter nbShown={1} showMore={jest.fn()} total={42} {...props} />,
  );
}
