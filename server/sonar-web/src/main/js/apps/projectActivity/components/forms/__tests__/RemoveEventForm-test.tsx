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
import ConfirmModal from '../../../../../components/controls/ConfirmModal';
import { mockAnalysisEvent } from '../../../../../helpers/mocks/project-activity';
import RemoveEventForm, { RemoveEventFormProps } from '../RemoveEventForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly confirm', () => {
  const onConfirm = jest.fn();
  const wrapper = shallowRender({ onConfirm });
  wrapper.find(ConfirmModal).prop('onConfirm')();
  expect(onConfirm).toHaveBeenCalledWith('foo', 'bar');
});

it('should correctly cancel', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });
  wrapper.find(ConfirmModal).prop('onClose')();
  expect(onClose).toHaveBeenCalled();
});

function shallowRender(props: Partial<RemoveEventFormProps> = {}) {
  return shallow(
    <RemoveEventForm
      analysisKey="foo"
      event={mockAnalysisEvent({ key: 'bar' })}
      header="Remove foo"
      onClose={jest.fn()}
      onConfirm={jest.fn()}
      removeEventQuestion="Remove foo?"
      {...props}
    />
  );
}
