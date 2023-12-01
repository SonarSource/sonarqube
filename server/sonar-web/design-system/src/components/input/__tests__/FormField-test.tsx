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
import { FCProps } from '~types/misc';
import { render } from '../../../helpers/testUtils';
import { FormField } from '../FormField';

it('should render correctly', () => {
  renderFormField({}, <input id="input" />);
  expect(screen.getByLabelText('Hello')).toBeInTheDocument();
});

it('should render with required and description', () => {
  renderFormField({ description: 'some description', required: true }, <input id="input" />);
  expect(screen.getByText('some description')).toBeInTheDocument();
  expect(screen.getByText('*')).toBeInTheDocument();
});

function renderFormField(
  props: Partial<FCProps<typeof FormField>> = {},
  children: any = <div>Fake input</div>,
) {
  return render(
    <FormField htmlFor="input" label="Hello" {...props}>
      {children}
    </FormField>,
  );
}
