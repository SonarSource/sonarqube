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
import { FCProps } from '../../../types/misc';
import { InputMultiSelect } from '../InputMultiSelect';

it('should render correctly', () => {
  renderInputMultiSelect();
  expect(screen.getByText('select')).toBeInTheDocument();
  expect(screen.queryByText('selected')).not.toBeInTheDocument();
  expect(screen.queryByText(/\d+/)).not.toBeInTheDocument();
});

it('should render correctly with a counter', () => {
  renderInputMultiSelect({ count: 42 });
  expect(screen.queryByText('select')).not.toBeInTheDocument();
  expect(screen.getByText('selected')).toBeInTheDocument();
  expect(screen.getByText('42')).toBeInTheDocument();
});

function renderInputMultiSelect(props: Partial<FCProps<typeof InputMultiSelect>> = {}) {
  render(<InputMultiSelect placeholder="select" selectedLabel="selected" {...props} />);
}
