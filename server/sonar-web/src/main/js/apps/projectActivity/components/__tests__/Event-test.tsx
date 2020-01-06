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
import { DeleteButton, EditButton } from 'sonar-ui-common/components/controls/buttons';
import { click } from 'sonar-ui-common/helpers/testUtils';
import { mockAnalysisEvent } from '../../../../helpers/testMocks';
import { Event, EventProps } from '../Event';
import ChangeEventForm from '../forms/ChangeEventForm';
import RemoveEventForm from '../forms/RemoveEventForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ canAdmin: true })).toMatchSnapshot('with admin options');
});

it('should correctly allow deletion', () => {
  expect(
    shallowRender({
      canAdmin: true,
      event: mockAnalysisEvent({ category: 'VERSION' }),
      isFirst: true
    })
      .find(DeleteButton)
      .exists()
  ).toBe(false);

  expect(
    shallowRender({ canAdmin: true, event: mockAnalysisEvent() })
      .find(DeleteButton)
      .exists()
  ).toBe(false);

  expect(
    shallowRender({ canAdmin: true })
      .find(DeleteButton)
      .exists()
  ).toBe(true);
});

it('should correctly allow edition', () => {
  expect(
    shallowRender({ canAdmin: true })
      .find(EditButton)
      .exists()
  ).toBe(true);

  expect(
    shallowRender({ canAdmin: true, isFirst: true })
      .find(EditButton)
      .exists()
  ).toBe(true);

  expect(
    shallowRender({ canAdmin: true, event: mockAnalysisEvent() })
      .find(EditButton)
      .exists()
  ).toBe(false);
});

it('should correctly show edit form', () => {
  const wrapper = shallowRender({ canAdmin: true });
  click(wrapper.find(EditButton));
  const changeEventForm = wrapper.find(ChangeEventForm);
  expect(changeEventForm.exists()).toBe(true);
  changeEventForm.prop('onClose')();
  expect(wrapper.find(ChangeEventForm).exists()).toBe(false);
});

it('should correctly show delete form', () => {
  const wrapper = shallowRender({ canAdmin: true });
  click(wrapper.find(DeleteButton));
  const removeEventForm = wrapper.find(RemoveEventForm);
  expect(removeEventForm.exists()).toBe(true);
  removeEventForm.prop('onClose')();
  expect(wrapper.find(RemoveEventForm).exists()).toBe(false);
});

function shallowRender(props: Partial<EventProps> = {}) {
  return shallow<Event>(
    <Event
      analysisKey="foo"
      event={mockAnalysisEvent({ category: 'OTHER' })}
      onChange={jest.fn()}
      onDelete={jest.fn()}
      {...props}
    />
  );
}
