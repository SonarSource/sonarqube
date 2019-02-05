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
import SetupProjectBox from '../SetupProjectBox';
import { waitAndUpdate, submit } from '../../../../helpers/testUtils';
import { provisionProject } from '../../../../api/alm-integration';
import { mockOrganizationWithAlm } from '../../../../helpers/testMocks';

jest.mock('../../../../api/alm-integration', () => ({
  provisionProject: jest
    .fn()
    .mockResolvedValue({ projects: [{ projectKey: 'awesome' }, { projectKey: 'foo' }] })
}));

it('should correctly create projects', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = shallowRender({ onProjectCreate });

  expect(wrapper).toMatchSnapshot();
  submit(wrapper.find('form'));
  expect(provisionProject).toBeCalledWith({
    installationKeys: ['github/awesome', 'github/foo'],
    organization: 'foo'
  });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith(['awesome', 'foo'], 'foo');
});

function shallowRender(props: Partial<SetupProjectBox['props']> = {}) {
  return shallow(
    <SetupProjectBox
      onProjectCreate={jest.fn()}
      onProvisionFail={jest.fn()}
      organization={mockOrganizationWithAlm({ subscription: 'FREE' })}
      selectedRepositories={[
        { label: 'Awesome Project', installationKey: 'github/awesome' },
        { label: 'Foo', installationKey: 'github/foo', private: true }
      ]}
      {...props}
    />
  );
}
