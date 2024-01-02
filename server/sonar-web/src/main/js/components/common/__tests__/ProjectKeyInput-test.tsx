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
import ValidationInput from '../../../components/controls/ValidationInput';
import ProjectKeyInput, { ProjectKeyInputProps } from '../ProjectKeyInput';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ projectKey: 'foo' })).toMatchSnapshot('with value');
  expect(
    shallowRender({ help: 'foo.help', label: 'foo.label', placeholder: 'foo.placeholder' })
  ).toMatchSnapshot('with label, help, and placeholder');
  expect(shallowRender({ touched: true })).toMatchSnapshot('valid');
  expect(shallowRender({ touched: true, error: 'bar.baz' })).toMatchSnapshot('invalid');
  expect(shallowRender({ touched: true, validating: true })).toMatchSnapshot('validating');
  expect(shallowRender({ autofocus: true })).toMatchSnapshot('autofocus');
});

it('should not display any status when the key is not defined', () => {
  const wrapper = shallowRender();
  const input = wrapper.find(ValidationInput);
  expect(input.props().isInvalid).toBe(false);
  expect(input.props().isValid).toBe(false);
});

function shallowRender(props: Partial<ProjectKeyInputProps> = {}) {
  return shallow<ProjectKeyInputProps>(
    <ProjectKeyInput onProjectKeyChange={jest.fn()} touched={false} {...props} />
  );
}
