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
/* eslint-disable camelcase */
import * as React from 'react';
import { shallow } from 'enzyme';
import PullRequestWidget from '../PullRequestWidget';
import { getPullRequestData } from '../../api';

jest.mock('../../api', () => ({
  getPullRequestData: jest.fn(() =>
    Promise.resolve({
      key: '1',
      isPullRequest: true,
      projectKey: 'foo',
      status: { qualityGateStatus: 'ERROR', codeSmells: 12, bugs: 2, vulnerabilities: 1 }
    })
  )
}));
jest.mock('../../utils', () => ({
  getRepoSettingsUrl: jest.fn(() =>
    Promise.resolve(
      'https://bitbucketcloud.org/{}/{repo_uuid}/admin/addon/admin/app-key/repository-config'
    )
  ),
  isRepoAdmin: jest.fn(() => true)
}));

const CONTEXT = { repoUuid: 'repo_uuid', prId: '1' };

beforeEach(() => {
  (getPullRequestData as jest.Mock<any>).mockClear();
});

it('should display correctly', async () => {
  const wrapper = shallow(<PullRequestWidget context={CONTEXT} />);
  expect(wrapper).toMatchSnapshot();
  expect(getPullRequestData).toHaveBeenCalledWith(CONTEXT);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should display a no analysis message', async () => {
  (getPullRequestData as jest.Mock<any>).mockImplementationOnce(() => Promise.reject('Not found'));
  const wrapper = shallow(<PullRequestWidget context={CONTEXT} />);
  expect(getPullRequestData).toHaveBeenCalledWith(CONTEXT);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should display a no issues message', async () => {
  (getPullRequestData as jest.Mock<any>).mockImplementationOnce(() =>
    Promise.resolve({
      key: '1',
      projectKey: 'foo',
      status: { qualityGateStatus: 'OK', codeSmells: 0, bugs: 0, vulnerabilities: 0 }
    })
  );
  const wrapper = shallow(<PullRequestWidget context={CONTEXT} />);
  expect(getPullRequestData).toHaveBeenCalledWith(CONTEXT);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should display projects not bound message', async () => {
  (getPullRequestData as jest.Mock<any>).mockImplementationOnce(
    jest.fn(() => Promise.reject({ errors: [{ msg: 'Repository not bound' }] }))
  );
  const wrapper = shallow(<PullRequestWidget context={CONTEXT} />);
  expect(getPullRequestData).toHaveBeenCalledWith(CONTEXT);

  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});
