/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import {
  checkPersonalAccessTokenIsValid,
  setAlmPersonalAccessToken
} from '../../../../api/alm-integrations';
import { SubmitButton } from '../../../../components/controls/buttons';
import {
  mockAlmSettingsInstance,
  mockBitbucketCloudAlmSettingsInstance
} from '../../../../helpers/mocks/alm-settings';
import { change, submit, waitAndUpdate } from '../../../../helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import PersonalAccessTokenForm from '../PersonalAccessTokenForm';

jest.mock('../../../../api/alm-integrations', () => ({
  checkPersonalAccessTokenIsValid: jest.fn().mockResolvedValue({ status: true }),
  setAlmPersonalAccessToken: jest.fn().mockResolvedValue({})
}));

it('should render correctly', async () => {
  expect(shallowRender()).toMatchSnapshot('no token needed');

  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce({ status: false });
  let wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('bitbucket');

  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce({ status: false });
  wrapper = shallowRender({ almSetting: mockBitbucketCloudAlmSettingsInstance() });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('bitbucket cloud');

  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce({ status: false });
  wrapper = shallowRender({
    almSetting: mockAlmSettingsInstance({ alm: AlmKeys.GitLab, url: 'https://gitlab.com/api/v4' })
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('gitlab');

  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce({ status: false });
  wrapper = shallowRender({
    almSetting: mockAlmSettingsInstance({
      alm: AlmKeys.GitLab,
      url: 'https://gitlabapi.unexpectedurl.org'
    })
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('gitlab with non-standard api path');
});

it('should correctly handle form interactions', async () => {
  const onPersonalAccessTokenCreated = jest.fn();
  const wrapper = shallowRender({ onPersonalAccessTokenCreated });

  await waitAndUpdate(wrapper);
  // Submit button disabled by default.
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(true);

  // Submit button enabled if there's a value.
  change(wrapper.find('input'), 'token');
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(false);

  // Expect correct calls to be made when submitting.
  submit(wrapper.find('form'));
  expect(onPersonalAccessTokenCreated).toBeCalled();
  expect(setAlmPersonalAccessToken).toBeCalledWith('key', 'token', undefined);
});

it('should correctly handle form for bitbucket interactions', async () => {
  const onPersonalAccessTokenCreated = jest.fn();
  const wrapper = shallowRender({
    almSetting: mockBitbucketCloudAlmSettingsInstance(),
    onPersonalAccessTokenCreated
  });

  await waitAndUpdate(wrapper);
  // Submit button disabled by default.
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(true);

  change(wrapper.find('#personal_access_token'), 'token');
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(true);

  // Submit button enabled if there's a value.
  change(wrapper.find('#username'), 'username');
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(false);

  // Expect correct calls to be made when submitting.
  submit(wrapper.find('form'));
  expect(onPersonalAccessTokenCreated).toBeCalled();
  expect(setAlmPersonalAccessToken).toBeCalledWith('key', 'token', 'username');
});

it('should show error when issue', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockRejectedValueOnce({});
  const wrapper = shallowRender({
    almSetting: mockBitbucketCloudAlmSettingsInstance()
  });

  await waitAndUpdate(wrapper);

  (checkPersonalAccessTokenIsValid as jest.Mock).mockRejectedValueOnce({});

  change(wrapper.find('#personal_access_token'), 'token');
  change(wrapper.find('#username'), 'username');

  // Expect correct calls to be made when submitting.
  submit(wrapper.find('form'));
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('issue submitting token');
});

function shallowRender(props: Partial<PersonalAccessTokenForm['props']> = {}) {
  return shallow<PersonalAccessTokenForm>(
    <PersonalAccessTokenForm
      almSetting={mockAlmSettingsInstance({
        alm: AlmKeys.BitbucketServer,
        url: 'http://www.example.com'
      })}
      onPersonalAccessTokenCreated={jest.fn()}
      resetPat={false}
      {...props}
    />
  );
}
