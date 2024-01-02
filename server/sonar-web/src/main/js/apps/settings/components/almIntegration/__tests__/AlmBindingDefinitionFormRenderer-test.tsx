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
import { ResetButtonLink } from '../../../../../components/controls/buttons';
import { mockGithubBindingDefinition } from '../../../../../helpers/mocks/alm-settings';
import { click, mockEvent } from '../../../../../helpers/testUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import AlmBindingDefinitionFormRenderer, {
  AlmBindingDefinitionFormProps,
} from '../AlmBindingDefinitionFormRenderer';
import GithubForm from '../GithubForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ submitting: true })).toMatchSnapshot('submitting');
  expect(shallowRender({ isUpdate: true })).toMatchSnapshot('editing');
  expect(shallowRender({ validationError: 'this is a validation error' })).toMatchSnapshot(
    'with validation error'
  );
});

it.each([[AlmKeys.Azure], [AlmKeys.GitHub], [AlmKeys.GitLab], [AlmKeys.BitbucketServer]])(
  'should render correctly for %s',
  (alm) => {
    expect(shallowRender({ alm })).toMatchSnapshot();
  }
);

it('should cancel properly', () => {
  const onCancel = jest.fn();
  const wrapper = shallowRender({ onCancel });

  click(wrapper.find(ResetButtonLink));
  expect(onCancel).toHaveBeenCalled();
});

it('should submit properly', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });

  const event: React.SyntheticEvent<HTMLFormElement> = mockEvent({ preventDefault: jest.fn() });

  wrapper.find('form').simulate('submit', event);

  expect(event.preventDefault).toHaveBeenCalled();
  expect(onSubmit).toHaveBeenCalled();
});

it('should handle field change', () => {
  const onFieldChange = jest.fn();
  const wrapper = shallowRender({ onFieldChange });

  wrapper.find(GithubForm).props().onFieldChange('key', 'test');

  expect(onFieldChange).toHaveBeenCalledWith('key', 'test');
});

function shallowRender(props: Partial<AlmBindingDefinitionFormProps> = {}) {
  return shallow(
    <AlmBindingDefinitionFormRenderer
      alm={AlmKeys.GitHub}
      isUpdate={false}
      canSubmit={false}
      submitting={false}
      formData={mockGithubBindingDefinition()}
      onCancel={jest.fn()}
      onSubmit={jest.fn()}
      onFieldChange={jest.fn()}
      bitbucketVariant={AlmKeys.BitbucketServer}
      onBitbucketVariantChange={jest.fn()}
      {...props}
    />
  );
}
