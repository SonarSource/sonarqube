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
import { Button, ResetButtonLink } from '../../../../components/controls/buttons';
import HotspotCommentPopup, { HotspotCommentPopupProps } from '../HotspotCommentPopup';

it('should render correclty', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should trigger update comment', () => {
  const props = {
    onCommentEditSubmit: jest.fn(),
  };
  const wrapper = shallowRender(props);
  wrapper.find('textarea').simulate('change', { target: { value: 'foo' } });
  wrapper.find(Button).simulate('click');

  expect(props.onCommentEditSubmit).toHaveBeenCalledWith('foo');
});

it('should trigger cancel update comment', () => {
  const props = {
    onCancelEdit: jest.fn(),
  };
  const wrapper = shallowRender(props);
  wrapper.find(ResetButtonLink).simulate('click');

  expect(props.onCancelEdit).toHaveBeenCalledTimes(1);
});

function shallowRender(props?: Partial<HotspotCommentPopupProps>) {
  return shallow(
    <HotspotCommentPopup
      markdownComment="test"
      onCancelEdit={jest.fn()}
      onCommentEditSubmit={jest.fn()}
      {...props}
    />
  );
}
