/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { unsubscribeFromEmailReport } from '../../../../api/component-report';
import { mockComponent } from '../../../../helpers/testMocks';
import SimpleModal from '../../../../sonar-ui-common/components/controls/SimpleModal';
import { waitAndUpdate } from '../../../../sonar-ui-common/helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import UnsubscribeEmailModal from '../UnsubscribeEmailModal';

jest.mock('../../../../api/component-report', () => ({
  unsubscribeFromEmailReport: jest.fn().mockResolvedValue(null)
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(diveIntoSimpleModal(shallowRender())).toMatchSnapshot('modal content');
  expect(diveIntoSimpleModal(shallowRender().setState({ success: true }))).toMatchSnapshot(
    'modal content, success'
  );
});

it('should correctly flag itself as (un)mounted', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  expect(instance.mounted).toBe(true);
  wrapper.unmount();
  expect(instance.mounted).toBe(false);
});

it('should correctly unsubscribe the user', async () => {
  const component = mockComponent({ key: 'foo' });
  const wrapper = shallowRender({ component });
  submitSimpleModal(wrapper);
  await waitAndUpdate(wrapper);

  expect(unsubscribeFromEmailReport).toHaveBeenCalledWith('foo');
  expect(wrapper.state().success).toBe(true);
});

function diveIntoSimpleModal(wrapper: ShallowWrapper) {
  return wrapper
    .find(SimpleModal)
    .dive()
    .children();
}

function submitSimpleModal(wrapper: ShallowWrapper) {
  wrapper
    .find(SimpleModal)
    .props()
    .onSubmit();
}

function shallowRender(props: Partial<UnsubscribeEmailModal['props']> = {}) {
  return shallow<UnsubscribeEmailModal>(
    <UnsubscribeEmailModal
      component={mockComponent({ qualifier: ComponentQualifier.Portfolio })}
      onClose={jest.fn()}
      {...props}
    />
  );
}
