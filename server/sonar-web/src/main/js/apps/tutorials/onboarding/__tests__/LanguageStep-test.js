/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import { shallow } from 'enzyme';
import LanguageStep from '../LanguageStep';

it('selects java', () => {
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageStep onDone={onDone} onReset={jest.fn()} />);

  wrapper.find('RadioToggle').prop('onCheck')('java');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper
    .find('RadioToggle')
    .at(1)
    .prop('onCheck')('maven');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
  expect(onDone).lastCalledWith({ language: 'java', javaBuild: 'maven' });

  wrapper
    .find('RadioToggle')
    .at(1)
    .prop('onCheck')('gradle');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
  expect(onDone).lastCalledWith({ language: 'java', javaBuild: 'gradle' });
});

it('selects c#', () => {
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageStep onDone={onDone} onReset={jest.fn()} />);

  wrapper.find('RadioToggle').prop('onCheck')('dotnet');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper.find('NewProjectForm').prop('onDone')('project-foo');
  expect(onDone).lastCalledWith({ language: 'dotnet', projectKey: 'project-foo' });
});

it('selects c-family', () => {
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageStep onDone={onDone} onReset={jest.fn()} sonarCloud={true} />);

  wrapper.find('RadioToggle').prop('onCheck')('c-family');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper
    .find('RadioToggle')
    .at(1)
    .prop('onCheck')('msvc');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper.find('NewProjectForm').prop('onDone')('project-foo');
  expect(onDone).lastCalledWith({
    language: 'c-family',
    cFamilyCompiler: 'msvc',
    projectKey: 'project-foo'
  });

  wrapper
    .find('RadioToggle')
    .at(1)
    .prop('onCheck')('clang-gcc');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper
    .find('RadioToggle')
    .at(2)
    .prop('onCheck')('linux');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper.find('NewProjectForm').prop('onDone')('project-foo');
  expect(onDone).lastCalledWith({
    language: 'c-family',
    cFamilyCompiler: 'clang-gcc',
    os: 'linux',
    projectKey: 'project-foo'
  });
});

it('selects other', () => {
  const onDone = jest.fn();
  const wrapper = shallow(<LanguageStep onDone={onDone} onReset={jest.fn()} />);

  wrapper.find('RadioToggle').prop('onCheck')('other');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper
    .find('RadioToggle')
    .at(1)
    .prop('onCheck')('mac');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper.find('NewProjectForm').prop('onDone')('project-foo');
  expect(onDone).lastCalledWith({ language: 'other', os: 'mac', projectKey: 'project-foo' });
});
