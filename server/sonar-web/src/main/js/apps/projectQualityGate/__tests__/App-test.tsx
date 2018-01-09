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
jest.mock('../../../api/quality-gates', () => ({
  associateGateWithProject: jest.fn(() => Promise.resolve()),
  dissociateGateWithProject: jest.fn(() => Promise.resolve()),
  fetchQualityGates: jest.fn(() => Promise.resolve({})),
  getGateForProject: jest.fn(() => Promise.resolve())
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

const associateGateWithProject = require('../../../api/quality-gates')
  .associateGateWithProject as jest.Mock<any>;

const dissociateGateWithProject = require('../../../api/quality-gates')
  .dissociateGateWithProject as jest.Mock<any>;

const fetchQualityGates = require('../../../api/quality-gates').fetchQualityGates as jest.Mock<any>;

const getGateForProject = require('../../../api/quality-gates').getGateForProject as jest.Mock<any>;

const addGlobalSuccessMessage = require('../../../app/utils/addGlobalSuccessMessage')
  .default as jest.Mock<any>;

const handleRequiredAuthorization = require('../../../app/utils/handleRequiredAuthorization')
  .default as jest.Mock<any>;

const component = {
  analysisDate: '',
  breadcrumbs: [],
  configuration: { showQualityGates: true },
  key: 'component',
  name: 'component',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

beforeEach(() => {
  associateGateWithProject.mockClear();
  dissociateGateWithProject.mockClear();
  addGlobalSuccessMessage.mockClear();
});

it('checks permissions', () => {
  handleRequiredAuthorization.mockClear();
  mount(
    <App component={{ ...component, configuration: undefined }} onComponentChange={jest.fn()} />
  );
  expect(handleRequiredAuthorization).toBeCalled();
});

it('fetches quality gates', () => {
  fetchQualityGates.mockClear();
  getGateForProject.mockClear();
  mount(<App component={component} onComponentChange={jest.fn()} />);
  expect(fetchQualityGates).toBeCalledWith({ organization: 'org' });
  expect(getGateForProject).toBeCalledWith({ organization: 'org', project: 'component' });
});

it('changes quality gate from custom to default', () => {
  const gate = randomGate('foo');
  const allGates = [gate, randomGate('bar', true), randomGate('baz')];
  const wrapper = mountRender(allGates, gate);
  wrapper.find('Form').prop<Function>('onChange')('foo', 'bar');
  expect(associateGateWithProject).toBeCalledWith({
    gateId: 'bar',
    organization: 'org',
    projectKey: 'component'
  });
});

it('changes quality gate from custom to custom', () => {
  const allGates = [randomGate('foo'), randomGate('bar', true), randomGate('baz')];
  const wrapper = mountRender(allGates, randomGate('foo'));
  wrapper.find('Form').prop<Function>('onChange')('foo', 'baz');
  expect(associateGateWithProject).toBeCalledWith({
    gateId: 'baz',
    organization: 'org',
    projectKey: 'component'
  });
});

it('changes quality gate from custom to none', () => {
  const allGates = [randomGate('foo'), randomGate('bar'), randomGate('baz')];
  const wrapper = mountRender(allGates, randomGate('foo'));
  wrapper.find('Form').prop<Function>('onChange')('foo', undefined);
  expect(dissociateGateWithProject).toBeCalledWith({
    gateId: 'foo',
    organization: 'org',
    projectKey: 'component'
  });
});

it('changes quality gate from none to custom', () => {
  const allGates = [randomGate('foo'), randomGate('bar'), randomGate('baz')];
  const wrapper = mountRender(allGates);
  wrapper.find('Form').prop<Function>('onChange')(undefined, 'baz');
  expect(associateGateWithProject).toBeCalledWith({
    gateId: 'baz',
    organization: 'org',
    projectKey: 'component'
  });
});

function randomGate(id: string, isDefault = false) {
  return { id, isDefault, name: id };
}

function mountRender(allGates: any[], gate?: any) {
  const wrapper = mount(<App component={component} onComponentChange={jest.fn()} />);
  wrapper.setState({ allGates, loading: false, gate });
  return wrapper;
}
