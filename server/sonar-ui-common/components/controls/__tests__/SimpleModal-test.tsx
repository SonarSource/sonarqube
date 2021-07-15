/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { click, waitAndUpdate } from '../../../helpers/testUtils';
import { Button } from '../buttons';
import SimpleModal, { ChildrenProps } from '../SimpleModal';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('closes', () => {
  const onClose = jest.fn();
  const children = ({ onCloseClick }: ChildrenProps) => (
    <Button onClick={onCloseClick}>close</Button>
  );
  const wrapper = shallowRender({ children, onClose });
  click(wrapper.find('Button'));
  expect(onClose).toBeCalled();
});

it('submits', async () => {
  const onSubmit = jest.fn(() => Promise.resolve());
  const children = ({ onSubmitClick, submitting }: ChildrenProps) => (
    <Button disabled={submitting} onClick={onSubmitClick}>
      close
    </Button>
  );
  const wrapper = shallowRender({ children, onSubmit });
  wrapper.instance().mounted = true;
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('Button'));
  expect(onSubmit).toBeCalled();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender({ children = () => <div />, ...props }: Partial<SimpleModal['props']> = {}) {
  return shallow<SimpleModal>(
    <SimpleModal header="" onClose={jest.fn()} onSubmit={jest.fn()} {...props}>
      {children}
    </SimpleModal>
  );
}
