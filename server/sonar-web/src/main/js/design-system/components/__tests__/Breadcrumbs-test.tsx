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
import { renderWithRouter } from '../../helpers/testUtils';
import { useResizeObserver } from '../../hooks/useResizeObserver';
import { BreadcrumbsFullWidth } from '../Breadcrumbs';
import { HoverLink } from '../Link';

jest.mock('../../hooks/useResizeObserver', () => ({
  useResizeObserver: jest.fn(() => [1000, undefined]),
}));

it('should display three breadcrumbs correctly', () => {
  renderWithRouter(
    <BreadcrumbsFullWidth>
      <HoverLink to="/first">first</HoverLink>
      <HoverLink to="/second">second</HoverLink>
      <HoverLink to="/third">third</HoverLink>
    </BreadcrumbsFullWidth>,
  );

  expect(screen.getAllByRole('link').length).toBe(3);
  expect(screen.getAllByTestId('chevron-right').length).toBe(2);
});

describe('when the container of the breadcrumbs is small(400px)', () => {
  const originalOffsetWidth = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'offsetWidth');
  beforeAll(() => {
    jest.mocked(useResizeObserver).mockImplementation(() => [400, undefined]);
    Object.defineProperty(HTMLElement.prototype, 'offsetWidth', { configurable: true, value: 110 });
  });

  afterAll(() => {
    Object.defineProperty(HTMLElement.prototype, 'offsetWidth', originalOffsetWidth as number);
  });

  it('should use the dropdown and hide the first breadcrumb when the width is 400', async () => {
    const content = (
      <BreadcrumbsFullWidth>
        <HoverLink to="/first-link-long">first link long</HoverLink>
        <HoverLink to="/second-link-long">second link long</HoverLink>
        <HoverLink to="/third-link-long">third link long</HoverLink>
        <HoverLink to="/fourth-link-long">fourth link long</HoverLink>
      </BreadcrumbsFullWidth>
    );

    const { user, rerender } = renderWithRouter(content);

    rerender(content);

    expect(screen.getByRole('button')).toBeVisible();

    expect(screen.getAllByRole('link').length).toBe(3);
    expect(screen.getAllByTestId('chevron-right').length).toBe(2);

    await user.click(screen.getByRole('button'));

    // 3 from breadcrumbs, 4 from dropdown
    expect(screen.getAllByRole('link').length).toBe(7);
  });
});
