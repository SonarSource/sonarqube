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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockGithubDefinition } from '../../../../../helpers/mocks/alm-settings';
import { GithubBindingDefinition } from '../../../../../types/alm-settings';
import AlmBindingDefinitionForm from '../AlmBindingDefinitionForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should reset if the props change', () => {
  const bindingDefinition = mockGithubDefinition();
  const wrapper = shallowRender({ bindingDefinition });

  wrapper.setState({ formData: { ...bindingDefinition, appId: 'newAppId' }, touched: true });
  wrapper.setProps({ bindingDefinition: { ...bindingDefinition } });
  expect(wrapper.state('touched')).toBe(true);

  wrapper.setProps({ bindingDefinition: mockGithubDefinition({ key: 'diffKey' }) });
  expect(wrapper.state('touched')).toBe(false);
});

it('should handle field changes', () => {
  const wrapper = shallowRender();

  const formData = {
    key: 'github - example',
    url: 'http://github.com',
    appId: '34812568251',
    privateKey: 'gs7df9g7d9fsg7x9df7g9xdg'
  };

  wrapper.instance().handleFieldChange('key', formData.key);
  wrapper.instance().handleFieldChange('url', formData.url);
  wrapper.instance().handleFieldChange('appId', formData.appId);
  wrapper.instance().handleFieldChange('privateKey', formData.privateKey);
  expect(wrapper.state().formData).toEqual(formData);
});

it('should handle form submit', async () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({
    onSubmit,
    bindingDefinition: { key: 'originalKey', appId: '', privateKey: '', url: '' }
  });
  const formData = {
    key: 'github instance',
    url: 'http://github.enterprise.com',
    appId: '34812568251',
    privateKey: 'gs7df9g7d9fsg7x9df7g9xdg'
  };
  wrapper.setState({ formData });
  await waitAndUpdate(wrapper);

  wrapper.instance().handleFormSubmit();

  expect(onSubmit).toHaveBeenCalledWith(formData, 'originalKey');
});

it('should handle cancelling', () => {
  const onCancel = jest.fn();
  const bindingDefinition = {
    appId: 'foo',
    key: 'bar',
    privateKey: 'baz',
    url: 'http://github.enterprise.com'
  };
  const wrapper = shallowRender({
    bindingDefinition,
    onCancel
  });

  wrapper.setState({ formData: mockGithubDefinition() });
  wrapper.instance().handleCancel();

  expect(wrapper.state().formData).toBe(bindingDefinition);
  expect(onCancel).toHaveBeenCalled();
});

it('should handle deleting', () => {
  const onDelete = jest.fn();
  const bindingDefinition = mockGithubDefinition();
  const wrapper = shallowRender({
    bindingDefinition,
    onDelete
  });

  wrapper.instance().handleDelete();
  expect(onDelete).toHaveBeenCalledWith(bindingDefinition.key);
});

it('should (dis)allow submit by validating its state', () => {
  const wrapper = shallowRender();
  expect(wrapper.instance().canSubmit()).toBe(false);

  wrapper.setState({ formData: mockGithubDefinition(), touched: true });
  expect(wrapper.instance().canSubmit()).toBe(true);

  wrapper.setState({ formData: mockGithubDefinition({ key: '' }), touched: true });
  wrapper.setProps({ hideKeyField: true });
  expect(wrapper.instance().canSubmit()).toBe(true);

  wrapper.setState({ formData: mockGithubDefinition({ url: '' }), touched: true });
  wrapper.setProps({ optionalFields: ['url'] });
  expect(wrapper.instance().canSubmit()).toBe(true);
});

function shallowRender(
  props: Partial<AlmBindingDefinitionForm<GithubBindingDefinition>['props']> = {}
) {
  return shallow<AlmBindingDefinitionForm<GithubBindingDefinition>>(
    <AlmBindingDefinitionForm
      bindingDefinition={{ appId: '', key: '', privateKey: '', url: '' }}
      onCancel={jest.fn()}
      onSubmit={jest.fn()}
      {...props}>
      {() => null}
    </AlmBindingDefinitionForm>
  );
}
