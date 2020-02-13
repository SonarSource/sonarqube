/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import { change, submit } from 'sonar-ui-common/helpers/testUtils';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../types/alm-settings';
import BitbucketPersonalAccessTokenForm, {
  BitbucketPersonalAccessTokenFormProps
} from '../BitbucketPersonalAccessTokenForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ submitting: true })).toMatchSnapshot('submitting');
  expect(shallowRender({ validationFailed: true })).toMatchSnapshot('validation failed');
});

it('should correctly handle form interactions', () => {
  const onPersonalAccessTokenCreate = jest.fn();
  const wrapper = shallowRender({ onPersonalAccessTokenCreate });

  // Submit button disabled by default.
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(true);

  // Submit button enabled if there's a value.
  change(wrapper.find('input'), 'token');
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(false);

  // Expect correct calls to be made when submitting.
  submit(wrapper.find('form'));
  expect(onPersonalAccessTokenCreate).toBeCalled();

  // If validation fails, we toggle the submitting flag and call useEffect()
  // to set the `touched` flag to false again. Trigger a re-render, and mock
  // useEffect(). This should de-activate the submit button again.
  jest.spyOn(React, 'useEffect').mockImplementationOnce(f => f());
  wrapper.setProps({ submitting: false });
  expect(wrapper.find(SubmitButton).prop('disabled')).toBe(true);
});

function shallowRender(props: Partial<BitbucketPersonalAccessTokenFormProps> = {}) {
  return shallow<BitbucketPersonalAccessTokenFormProps>(
    <BitbucketPersonalAccessTokenForm
      bitbucketSetting={mockAlmSettingsInstance({
        alm: AlmKeys.Bitbucket,
        url: 'http://www.example.com'
      })}
      onPersonalAccessTokenCreate={jest.fn()}
      validationFailed={false}
      {...props}
    />
  );
}
