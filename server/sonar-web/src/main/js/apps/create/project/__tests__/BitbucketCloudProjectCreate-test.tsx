/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { checkPersonalAccessTokenIsValid } from '../../../../api/alm-integrations';
import { mockBitbucketCloudAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import BitbucketCloudProjectCreate from '../BitbucketCloudProjectCreate';

jest.mock('../../../../api/alm-integrations', () => {
  return {
    checkPersonalAccessTokenIsValid: jest.fn().mockResolvedValue({ status: true }),
    setAlmPersonalAccessToken: jest.fn().mockResolvedValue(null)
  };
});

it('Should render correctly', async () => {
  let wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  (checkPersonalAccessTokenIsValid as jest.Mock).mockRejectedValueOnce({});
  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('Need App password');
});

it('Should handle app password correctly', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  await wrapper.instance().handlePersonalAccessTokenCreated();
  expect(wrapper.state().showPersonalAccessTokenForm).toBe(false);
});

function shallowRender(props?: Partial<BitbucketCloudProjectCreate['props']>) {
  return shallow<BitbucketCloudProjectCreate>(
    <BitbucketCloudProjectCreate
      onProjectCreate={jest.fn()}
      loadingBindings={false}
      location={mockLocation()}
      canAdmin={true}
      router={mockRouter()}
      settings={[mockBitbucketCloudAlmSettingsInstance()]}
      {...props}
    />
  );
}
