/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import ModalValidationField from '../ModalValidationField';

it('should display the field without any error/validation', () => {
  expect(getWrapper({ description: 'Describe Foo.', touched: false })).toMatchSnapshot();
  expect(getWrapper({ dirty: false })).toMatchSnapshot();
});

it('should display the field as valid', () => {
  expect(getWrapper({ error: undefined })).toMatchSnapshot();
});

it('should display the field with an error', () => {
  expect(getWrapper()).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <ModalValidationField
      dirty={true}
      error="Is required"
      label={<label>Foo</label>}
      touched={true}
      {...props}>
      {({ className }) => <input className={className} type="text" />}
    </ModalValidationField>
  );
}
