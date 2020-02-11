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
import Radio from 'sonar-ui-common/components/controls/Radio';
import { change, click } from 'sonar-ui-common/helpers/testUtils';
import { HotspotStatusOption } from '../../../../../types/security-hotspots';
import StatusSelectionRenderer, { StatusSelectionRendererProps } from '../StatusSelectionRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(
    shallowRender({ submitDisabled: true })
      .find(SubmitButton)
      .props().disabled
  ).toBe(true);
});

it('should call proper callbacks on actions', () => {
  const onCommentChange = jest.fn();
  const onStatusChange = jest.fn();
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onCommentChange, onStatusChange, onSubmit });

  change(wrapper.find('textarea'), 'TATA');
  expect(onCommentChange).toHaveBeenCalledWith('TATA');

  wrapper
    .find(Radio)
    .first()
    .props()
    .onCheck(HotspotStatusOption.SAFE);
  expect(onStatusChange).toHaveBeenCalledWith(HotspotStatusOption.SAFE);

  click(wrapper.find(SubmitButton));
  expect(onSubmit).toHaveBeenCalled();
});

function shallowRender(props?: Partial<StatusSelectionRendererProps>) {
  return shallow<StatusSelectionRendererProps>(
    <StatusSelectionRenderer
      comment="TEST-COMMENT"
      loading={false}
      onCommentChange={jest.fn()}
      onStatusChange={jest.fn()}
      onSubmit={jest.fn()}
      selectedStatus={HotspotStatusOption.TO_REVIEW}
      submitDisabled={false}
      {...props}
    />
  );
}
