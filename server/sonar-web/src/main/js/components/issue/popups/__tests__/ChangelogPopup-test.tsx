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
import { getIssueChangelog } from '../../../../api/issues';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import ChangelogPopup from '../ChangelogPopup';

jest.mock('../../../../api/issues', () => ({
  getIssueChangelog: jest.fn().mockResolvedValue({
    changelog: [
      {
        creationDate: '2017-03-01T09:36:01+0100',
        user: 'john.doe',
        isUserActive: true,
        userName: 'John Doe',
        avatar: 'gravatarhash',
        diffs: [{ key: 'severity', newValue: 'MINOR', oldValue: 'CRITICAL' }],
      },
    ],
  }),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render the changelog popup correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getIssueChangelog).toHaveBeenCalledWith('issuekey');
  expect(wrapper).toMatchSnapshot();
});

it('should render the changelog popup when we have a deleted user', async () => {
  (getIssueChangelog as jest.Mock).mockResolvedValueOnce({
    changelog: [
      {
        creationDate: '2017-03-01T09:36:01+0100',
        user: 'john.doe',
        isUserActive: false,
        diffs: [{ key: 'severity', newValue: 'MINOR', oldValue: 'CRITICAL' }],
      },
    ],
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render the changelog popup when change was triggered by a webhook with external user', async () => {
  (getIssueChangelog as jest.Mock).mockResolvedValueOnce({
    changelog: [
      {
        creationDate: '2017-03-01T09:36:01+0100',
        user: null,
        isUserActive: false,
        diffs: [{ key: 'severity', newValue: 'MINOR', oldValue: 'CRITICAL' }],
        webhookSource: 'GitHub',
        externalUser: 'toto@github.com',
      },
    ],
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render the changelog popup when change was triggered by a webhook without user', async () => {
  (getIssueChangelog as jest.Mock).mockResolvedValueOnce({
    changelog: [
      {
        creationDate: '2017-03-01T09:36:01+0100',
        user: null,
        isUserActive: false,
        diffs: [{ key: 'severity', newValue: 'MINOR', oldValue: 'CRITICAL' }],
        webhookSource: 'GitHub',
      },
    ],
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render the changelog popup with SQ user when both SQ and external user are presents', async () => {
  (getIssueChangelog as jest.Mock).mockResolvedValueOnce({
    changelog: [
      {
        creationDate: '2017-03-01T09:36:01+0100',
        user: 'toto@sonarqube.com',
        isUserActive: false,
        diffs: [{ key: 'severity', newValue: 'MINOR', oldValue: 'CRITICAL' }],
        webhookSource: 'GitHub',
        externalUser: 'toto@github.com',
      },
    ],
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<ChangelogPopup['props']> = {}) {
  return shallow(
    <ChangelogPopup
      issue={{
        key: 'issuekey',
        author: 'john.david.dalton@gmail.com',
        creationDate: '2017-03-01T09:36:01+0100',
      }}
      {...props}
    />
  );
}
