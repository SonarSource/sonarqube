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
import { shallow } from 'enzyme';
import * as React from 'react';
import ValidationInput, {
  ValidationInputErrorPlacement,
  ValidationInputProps,
} from '../ValidationInput';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ help: 'Help message', isValid: false })).toMatchSnapshot('with help');
  expect(
    shallowRender({
      description: <div>My description</div>,
      error: 'Field error message',
      isInvalid: true,
      isValid: false,
      required: false,
    })
  ).toMatchSnapshot('with error');
  expect(
    shallowRender({
      error: 'Field error message',
      errorPlacement: ValidationInputErrorPlacement.Bottom,
      isInvalid: true,
      isValid: false,
    })
  ).toMatchSnapshot('error under the input');
  expect(shallowRender({ labelHtmlFor: undefined, label: undefined })).toMatchSnapshot('no label');
});

function shallowRender(props: Partial<ValidationInputProps> = {}) {
  return shallow<ValidationInputProps>(
    <ValidationInput
      description="My description"
      error={undefined}
      labelHtmlFor="field-id"
      isInvalid={false}
      isValid={true}
      label="Field label"
      required={true}
      {...props}
    >
      <div />
    </ValidationInput>
  );
}
