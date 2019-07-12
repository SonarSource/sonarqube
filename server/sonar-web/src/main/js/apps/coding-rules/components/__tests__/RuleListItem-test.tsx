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
import { mockEvent, mockQualityProfile, mockRule } from '../../../../helpers/testMocks';
import RuleListItem from '../RuleListItem';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should open rule', () => {
  const onOpen = jest.fn();
  const wrapper = shallowRender({ onOpen });
  wrapper.find('Link').prop<Function>('onClick')(mockEvent({ button: 0 }));
  expect(onOpen).toBeCalledWith('javascript:S1067');
});

it('should render deactivate button', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  expect(instance.renderDeactivateButton('NONE')).toMatchSnapshot();
  expect(instance.renderDeactivateButton('', 'coding_rules.need_extend_or_copy')).toMatchSnapshot();
});

describe('renderActions', () => {
  it('should be null when there is no selected profile', () => {
    const wrapper = shallowRender({
      isLoggedIn: true
    });

    expect(wrapper.instance().renderActions()).toBeNull();
  });

  it('should be null when I am not logged in', () => {
    const wrapper = shallowRender({
      isLoggedIn: false,
      selectedProfile: mockQualityProfile()
    });

    expect(wrapper.instance().renderActions()).toBeNull();
  });

  it('should be null when the user does not have the sufficient permissions', () => {
    const wrapper = shallowRender({
      isLoggedIn: true,
      selectedProfile: mockQualityProfile()
    });

    expect(wrapper.instance().renderActions()).toBeNull();
  });

  it('should disable the button when I am on a built-in profile', () => {
    const wrapper = shallowRender({
      selectedProfile: mockQualityProfile({
        actions: {
          copy: true
        },
        isBuiltIn: true
      })
    });

    expect(wrapper.instance().renderActions()).toMatchSnapshot();
  });

  it('should render the deactivate button', () => {
    const wrapper = shallowRender({
      activation: {
        inherit: 'NONE',
        severity: 'warning'
      },
      selectedProfile: mockQualityProfile({
        actions: {
          edit: true
        },
        isBuiltIn: false
      })
    });

    expect(wrapper.instance().renderActions()).toMatchSnapshot();
  });

  it('should render the activate button', () => {
    const wrapper = shallowRender({
      rule: mockRule({
        isTemplate: false
      }),
      selectedProfile: mockQualityProfile({
        actions: {
          edit: true
        },
        isBuiltIn: false
      })
    });

    expect(wrapper.instance().renderActions()).toMatchSnapshot();
  });
});

function shallowRender(props?: Partial<RuleListItem['props']>) {
  return shallow<RuleListItem>(
    <RuleListItem
      isLoggedIn={true}
      onActivate={jest.fn()}
      onDeactivate={jest.fn()}
      onFilterChange={jest.fn()}
      onOpen={jest.fn()}
      organization="org"
      rule={mockRule()}
      selected={false}
      {...props}
    />
  );
}
