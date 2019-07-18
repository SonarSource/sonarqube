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
import { shallow } from 'enzyme';
import * as React from 'react';
import { isSonarCloud } from '../../../../helpers/system';
import LanguageForm from '../LanguageForm';

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

beforeEach(() => {
  (isSonarCloud as jest.Mock<any>).mockImplementation(() => false);
});

it('selects java', () => {
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageForm onDone={onDone} onReset={jest.fn()} />);

  (wrapper.find('RadioToggle').prop('onCheck') as Function)('java');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('selects c#', () => {
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageForm onDone={onDone} onReset={jest.fn()} />);

  (wrapper.find('RadioToggle').prop('onCheck') as Function)('dotnet');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  (wrapper.find('NewProjectForm').prop('onDone') as Function)('project-foo');
  expect(onDone).lastCalledWith({ language: 'dotnet', projectKey: 'project-foo' });
});

it('selects c-family', () => {
  (isSonarCloud as jest.Mock<any>).mockImplementation(() => true);
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageForm onDone={onDone} onReset={jest.fn()} />);

  (wrapper.find('RadioToggle').prop('onCheck') as Function)('c-family');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('selects other', () => {
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageForm onDone={onDone} onReset={jest.fn()} />);

  (wrapper.find('RadioToggle').prop('onCheck') as Function)('other');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});
