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
/* eslint-disable import/order */
import * as React from 'react';
import { shallow } from 'enzyme';
import App from '../App';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/system', () => ({
  getMigrationStatus: jest.fn(),
  getSystemStatus: jest.fn(),
  migrateDatabase: jest.fn()
}));

jest.useFakeTimers();

const getMigrationStatus = require('../../../../api/system').getMigrationStatus as jest.Mock;
const getSystemStatus = require('../../../../api/system').getSystemStatus as jest.Mock;
const migrateDatabase = require('../../../../api/system').migrateDatabase as jest.Mock;

const location = { query: {} };

beforeEach(() => {
  getMigrationStatus.mockClear();
  getSystemStatus.mockClear();
  migrateDatabase.mockClear();
});

afterEach(() => {
  jest.clearAllTimers();
});

describe('Maintenance Page', () => {
  ['UP', 'DOWN', 'STARTING', 'DB_MIGRATION_NEEDED', 'DB_MIGRATION_RUNNING'].forEach(status => {
    it(`should render ${status} status`, async () => {
      getSystemStatus.mockImplementationOnce(() => Promise.resolve({ status }));
      await checkApp(false);
    });
  });

  it('should render OFFLINE status', async () => {
    getSystemStatus.mockImplementationOnce(() => Promise.reject(undefined));
    await checkApp(false);
  });

  it('should poll status', async () => {
    getSystemStatus.mockImplementationOnce(() =>
      Promise.resolve({ status: 'DB_MIGRATION_RUNNING' })
    );
    const wrapper = shallow(<App location={location} setup={false} />);
    await waitAndUpdate(wrapper);
    expect(getSystemStatus).toBeCalled();

    getSystemStatus.mockClear();
    getSystemStatus.mockImplementationOnce(() =>
      Promise.resolve({ status: 'DB_MIGRATION_RUNNING' })
    );
    jest.runOnlyPendingTimers();
    await waitAndUpdate(wrapper);
    expect(getSystemStatus).toBeCalled();

    getSystemStatus.mockClear();
    getSystemStatus.mockImplementationOnce(() =>
      Promise.resolve({ status: 'DB_MIGRATION_RUNNING' })
    );
    jest.runOnlyPendingTimers();
    await waitAndUpdate(wrapper);
    expect(getSystemStatus).toBeCalled();
  });

  it('should open previous page', async () => {
    getSystemStatus.mockImplementationOnce(() => Promise.resolve({ status: 'STARTING' }));
    const wrapper = shallow(<App location={location} setup={false} />);
    const loadPreviousPage = jest.fn();
    (wrapper.instance() as App).loadPreviousPage = loadPreviousPage;
    await waitAndUpdate(wrapper);

    getSystemStatus.mockImplementationOnce(() => Promise.resolve({ status: 'UP' }));
    jest.runOnlyPendingTimers();
    await waitAndUpdate(wrapper);
    expect(loadPreviousPage).toBeCalled();
  });
});

describe('Setup Page', () => {
  ['NO_MIGRATION', 'NOT_SUPPORTED', 'MIGRATION_SUCCEEDED', 'MIGRATION_FAILED'].forEach(state => {
    it(`should render ${state} state`, async () => {
      getMigrationStatus.mockImplementationOnce(() =>
        Promise.resolve({ message: 'message', startedAt: '2017-01-02T00:00:00.000Z', state })
      );
      await checkApp(true);
    });
  });

  it('should start migration', async () => {
    getMigrationStatus.mockImplementationOnce(() =>
      Promise.resolve({ state: 'MIGRATION_REQUIRED' })
    );
    migrateDatabase.mockImplementationOnce(() =>
      Promise.resolve({ startedAt: '2017-01-02T00:00:00.000Z', state: 'MIGRATION_RUNNING' })
    );
    const wrapper = shallow(<App location={location} setup={true} />);
    await waitAndUpdate(wrapper);
    expect(wrapper).toMatchSnapshot();

    click(wrapper.find('button'));
    expect(migrateDatabase).toBeCalled();
    await waitAndUpdate(wrapper);
    expect(wrapper).toMatchSnapshot();
  });
});

async function checkApp(setup: boolean) {
  const wrapper = shallow(<App location={location} setup={setup} />);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
}
