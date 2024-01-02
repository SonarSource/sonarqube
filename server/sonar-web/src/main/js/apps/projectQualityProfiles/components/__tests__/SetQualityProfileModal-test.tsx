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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import Radio from '../../../../components/controls/Radio';
import Select from '../../../../components/controls/Select';
import SimpleModal from '../../../../components/controls/SimpleModal';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import SetQualityProfileModal, { SetQualityProfileModalProps } from '../SetQualityProfileModal';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ usesDefault: true })).toMatchSnapshot('inherits system default');
  expect(shallowRender({ component: mockComponent() })).toMatchSnapshot('needs reanalysis');
});

it('should correctly handle changes', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit }, false);

  diveIntoSimpleModal(wrapper).find(Radio).at(0).props().onCheck('');
  submitSimpleModal(wrapper);
  expect(onSubmit).toHaveBeenLastCalledWith(undefined, 'foo');

  diveIntoSimpleModal(wrapper).find(Radio).at(1).props().onCheck('');
  diveIntoSimpleModal(wrapper).find(Select).props().onChange({ value: 'bar' });
  submitSimpleModal(wrapper);
  expect(onSubmit).toHaveBeenLastCalledWith('bar', 'foo');

  const change = diveIntoSimpleModal(wrapper).find(Select).props().onChange;

  expect(change).toBeDefined();

  change!({ value: 'bar' });
  submitSimpleModal(wrapper);
  expect(onSubmit).toHaveBeenLastCalledWith('bar', 'foo');
});

function diveIntoSimpleModal(wrapper: ShallowWrapper) {
  return wrapper.find(SimpleModal).dive().children();
}

function submitSimpleModal(wrapper: ShallowWrapper) {
  wrapper.find(SimpleModal).props().onSubmit();
}

function shallowRender(props: Partial<SetQualityProfileModalProps> = {}, dive = true) {
  const wrapper = shallow<SetQualityProfileModalProps>(
    <SetQualityProfileModal
      availableProfiles={[
        mockQualityProfile({ key: 'foo', isDefault: true, language: 'js' }),
        mockQualityProfile({ key: 'bar', language: 'js', activeRuleCount: 0 }),
      ]}
      component={mockComponent({ qualityProfiles: [{ key: 'foo', name: 'Foo', language: 'js' }] })}
      currentProfile={mockQualityProfile({ key: 'foo', language: 'js' })}
      onClose={jest.fn()}
      onSubmit={jest.fn()}
      usesDefault={false}
      {...props}
    />
  );

  return dive ? diveIntoSimpleModal(wrapper) : wrapper;
}
