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
/* eslint-disable import/first, import/order */
jest.mock('../../../api/quality-profiles', () => ({
  associateProject: jest.fn(() => Promise.resolve()),
  dissociateProject: jest.fn(() => Promise.resolve()),
  searchQualityProfiles: jest.fn(() => Promise.resolve())
}));

jest.mock('../../../app/utils/addGlobalSuccessMessage', () => ({
  default: jest.fn()
}));

jest.mock('../../../app/utils/handleRequiredAuthorization', () => ({
  default: jest.fn()
}));

import * as React from 'react';
import { mount } from 'enzyme';
import App from '../App';

const associateProject = require('../../../api/quality-profiles').associateProject as jest.Mock<
  any
>;

const dissociateProject = require('../../../api/quality-profiles').dissociateProject as jest.Mock<
  any
>;

const searchQualityProfiles = require('../../../api/quality-profiles')
  .searchQualityProfiles as jest.Mock<any>;

const addGlobalSuccessMessage = require('../../../app/utils/addGlobalSuccessMessage')
  .default as jest.Mock<any>;

const handleRequiredAuthorization = require('../../../app/utils/handleRequiredAuthorization')
  .default as jest.Mock<any>;

const component = {
  analysisDate: '',
  breadcrumbs: [],
  configuration: { showQualityProfiles: true },
  key: 'foo',
  name: 'foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

it('checks permissions', () => {
  handleRequiredAuthorization.mockClear();
  mount(<App component={{ ...component, configuration: undefined }} />);
  expect(handleRequiredAuthorization).toBeCalled();
});

it('fetches profiles', () => {
  searchQualityProfiles.mockClear();
  mount(<App component={component} />);
  expect(searchQualityProfiles.mock.calls).toHaveLength(2);
  expect(searchQualityProfiles).toBeCalledWith({ organization: 'org' });
  expect(searchQualityProfiles).toBeCalledWith({ organization: 'org', project: 'foo' });
});

it('changes profile', () => {
  associateProject.mockClear();
  dissociateProject.mockClear();
  addGlobalSuccessMessage.mockClear();
  const wrapper = mount(<App component={component} />);

  const fooJava = randomProfile('foo-java', 'java');
  const fooJs = randomProfile('foo-js', 'js');
  const allProfiles = [
    fooJava,
    randomProfile('bar-java', 'java'),
    randomProfile('baz-java', 'java', true),
    fooJs
  ];
  const profiles = [fooJava, fooJs];
  wrapper.setState({ allProfiles, loading: false, profiles });

  wrapper.find('Table').prop<Function>('onChangeProfile')('foo-java', 'bar-java');
  expect(associateProject).toBeCalledWith('bar-java', 'foo');

  wrapper.find('Table').prop<Function>('onChangeProfile')('foo-java', 'baz-java');
  expect(dissociateProject).toBeCalledWith('foo-java', 'foo');
});

function randomProfile(key: string, language: string, isDefault = false) {
  return {
    activeRuleCount: 17,
    activeDeprecatedRuleCount: 0,
    isDefault,
    key,
    name: key,
    language,
    languageName: language,
    organization: 'org'
  };
}
