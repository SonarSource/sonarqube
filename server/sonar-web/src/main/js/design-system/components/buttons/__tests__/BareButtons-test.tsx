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
import { render } from '../../../helpers/testUtils';
import { CodeViewerExpander } from '../BareButtons';

it('renders CodeViewerExpander correctly when direction is UP', () => {
  render(<CodeViewerExpander direction="UP">Hello</CodeViewerExpander>);
  const content = screen.getByRole('button', { name: 'Hello' });
  expect(content).toHaveStyle({
    'border-top': 'none',
    'border-bottom': '1px solid rgb(221,221,221)',
  });
});

it('renders CodeViewerExpander correctly when direction is DOWN', () => {
  render(<CodeViewerExpander direction="DOWN">Hello</CodeViewerExpander>);
  const content = screen.getByRole('button', { name: 'Hello' });
  expect(content).toHaveStyle({
    'border-bottom': 'none',
    'border-top': '1px solid rgb(221,221,221)',
  });
});
