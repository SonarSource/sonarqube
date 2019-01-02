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
import * as React from 'react';
import { shallow } from 'enzyme';
import SimpleModal, { ChildrenProps } from '../SimpleModal';
import { Button } from '../../ui/buttons';
import { click, waitAndUpdate } from '../../../helpers/testUtils';

it('renders', () => {
  const inner = () => <div />;
  expect(
    shallow(
      <SimpleModal header="" onClose={jest.fn()} onSubmit={jest.fn()}>
        {inner}
      </SimpleModal>
    )
  ).toMatchSnapshot();
});

it('closes', () => {
  const onClose = jest.fn();
  const inner = ({ onCloseClick }: ChildrenProps) => <Button onClick={onCloseClick}>close</Button>;
  const wrapper = shallow(
    <SimpleModal header="" onClose={onClose} onSubmit={jest.fn()}>
      {inner}
    </SimpleModal>
  );
  click(wrapper.find('Button'));
  expect(onClose).toBeCalled();
});

it('submits', async () => {
  const onSubmit = jest.fn(() => Promise.resolve());
  const inner = ({ onSubmitClick, submitting }: ChildrenProps) => (
    <Button disabled={submitting} onClick={onSubmitClick}>
      close
    </Button>
  );
  const wrapper = shallow(
    <SimpleModal header="" onClose={jest.fn()} onSubmit={onSubmit}>
      {inner}
    </SimpleModal>
  );
  (wrapper.instance() as SimpleModal).mounted = true;
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('Button'));
  expect(onSubmit).toBeCalled();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});
