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
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';

import { SizeLabel } from '../../types/measures';
import { SizeIndicator } from '../SizeIndicator';

it.each([
  [100, 'XS'],
  [1100, 'S'],
  [20_000, 'M'],
  [200_000, 'L'],
  [1_000_000, 'XL'],
])('should display SizeIndicator with size', (value: number, letter: SizeLabel) => {
  setupWithProps({ value });
  expect(screen.getByText(letter)).toBeInTheDocument();
});

function setupWithProps(props: Partial<FCProps<typeof SizeIndicator>> = {}) {
  return render(<SizeIndicator value={0} {...props} />);
}
