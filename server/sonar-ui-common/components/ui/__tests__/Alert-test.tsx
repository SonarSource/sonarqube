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
import { Alert, AlertProps } from '../Alert';

it('should render properly', () => {
  expect(shallowRender({ variant: 'error' })).toMatchSnapshot();
});

it('verification of all variants of alert', () => {
  const variants: AlertProps['variant'][] = ['error', 'warning', 'success', 'info', 'loading'];
  variants.forEach((variant) => {
    const wrapper = shallowRender({ variant });
    expect(wrapper.prop('variantInfo')).toMatchSnapshot();
  });
});

it('should render inline alert', () => {
  expect(shallowRender({ display: 'inline' }).find('Styled(div)[isInline=true]').exists()).toBe(
    true
  );
});

it('should render banner alert', () => {
  expect(shallowRender({ display: 'banner' }).find('Styled(div)[isBanner=true]').exists()).toBe(
    true
  );
});

it('should render banner alert with correct css', () => {
  expect(shallowRender({ display: 'banner' }).render()).toMatchSnapshot();
});

function shallowRender(props: Partial<AlertProps>) {
  return shallow(
    <Alert className="alert-test" id="error-message" variant="error" {...props}>
      This is an error!
    </Alert>
  );
}
