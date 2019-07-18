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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getIssueChangelog } from '../../../../api/issues';
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
        diffs: [{ key: 'severity', newValue: 'MINOR', oldValue: 'CRITICAL' }]
      }
    ]
  })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render the changelog popup correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getIssueChangelog).toBeCalledWith('issuekey');
  expect(wrapper).toMatchSnapshot();
});

it('should render the changelog popup when we have a deleted user', async () => {
  (getIssueChangelog as jest.Mock).mockResolvedValueOnce({
    changelog: [
      {
        creationDate: '2017-03-01T09:36:01+0100',
        user: 'john.doe',
        isUserActive: false,
        diffs: [{ key: 'severity', newValue: 'MINOR', oldValue: 'CRITICAL' }]
      }
    ]
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
        creationDate: '2017-03-01T09:36:01+0100'
      }}
      {...props}
    />
  );
}
