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
import { RuleDetailsIssues } from '../RuleDetailsIssues';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { getFacet } from '../../../../api/issues';

jest.mock('../../../../api/issues', () => ({
  getFacet: jest.fn().mockResolvedValue({
    facet: [{ count: 13, val: 'sample-key' }, { count: 5, val: 'example-key' }],
    response: {
      components: [{ key: 'sample-key', name: 'Sample' }, { key: 'example-key', name: 'Example' }],
      paging: { total: 18 }
    }
  })
}));

beforeEach(() => {
  (getFacet as jest.Mock).mockClear();
});

it('should fetch issues and render', async () => {
  await check('BUG', undefined);
});

it('should handle hotspot rules', async () => {
  await check('SECURITY_HOTSPOT', ['VULNERABILITY', 'SECURITY_HOTSPOT']);
});

async function check(ruleType: T.RuleType, requestedTypes: T.RuleType[] | undefined) {
  const wrapper = shallow(
    <RuleDetailsIssues
      appState={{ branchesEnabled: false }}
      organization="org"
      ruleDetails={{ key: 'foo', type: ruleType }}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getFacet).toBeCalledWith(
    {
      organization: 'org',
      resolved: 'false',
      rules: 'foo',
      types: requestedTypes && requestedTypes.join()
    },
    'projects'
  );
}
