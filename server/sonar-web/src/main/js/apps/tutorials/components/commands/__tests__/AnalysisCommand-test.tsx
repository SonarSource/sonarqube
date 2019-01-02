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
import AnalysisCommand from '../AnalysisCommand';

jest.mock('../../../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(true)
}));

jest.mock('../../../../../helpers/urls', () => ({
  getHostUrl: () => 'HOST'
}));

it('display java command', () => {
  expect(
    getWrapper({ languageConfig: { language: 'java', javaBuild: 'gradle' } })
  ).toMatchSnapshot();
  expect(
    getWrapper({ languageConfig: { language: 'java', javaBuild: 'maven' } })
  ).toMatchSnapshot();
});

it('display c# command', () => {
  expect(
    getWrapper({ languageConfig: { language: 'dotnet', projectKey: 'project-foo' } })
  ).toMatchSnapshot();
});

it('display c-family command', () => {
  expect(
    getWrapper({
      languageConfig: { language: 'c-family', cFamilyCompiler: 'msvc', projectKey: 'project-foo' }
    })
  ).toMatchSnapshot();
  expect(
    getWrapper({
      languageConfig: {
        language: 'c-family',
        cFamilyCompiler: 'clang-gcc',
        os: 'linux',
        projectKey: 'project-foo'
      }
    })
  ).toMatchSnapshot();
});

it('display others command', () => {
  expect(
    getWrapper({
      languageConfig: { language: 'other', os: 'window', projectKey: 'project-foo' }
    })
  ).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(<AnalysisCommand languageConfig={{}} token="myToken" {...props} />);
}
