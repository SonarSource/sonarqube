/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import {
  ReactAsyncSelectProps,
  ReactCreatableSelectProps,
  ReactSelectProps
} from 'react-select-legacy';
import SelectLegacy, { AsyncSelectLegacy, CreatableLegacy, WithInnerRef } from '../SelectLegacy';

describe('Select', () => {
  it('should render correctly', () => {
    return new Promise<void>((resolve, reject) => {
      expect(shallowRender()).toMatchSnapshot('default');
      expect(shallowRender({ clearable: true, value: undefined })).toMatchSnapshot(
        'disable clear button if no value'
      );

      const clearRenderFn = shallowRender().props().clearRenderer;
      if (!clearRenderFn) {
        reject();
        return;
      }
      expect(clearRenderFn()).toMatchSnapshot('clear button');

      resolve();
    });
  });

  function shallowRender(props: Partial<WithInnerRef & ReactSelectProps> = {}) {
    return shallow<WithInnerRef & ReactSelectProps>(<SelectLegacy value="foo" {...props} />);
  }
});

describe('Creatable', () => {
  it('should render correctly', () => {
    return new Promise<void>((resolve, reject) => {
      expect(shallowRender()).toMatchSnapshot('default');

      const clearRenderFn = shallowRender().props().clearRenderer;
      if (!clearRenderFn) {
        reject();
        return;
      }
      expect(clearRenderFn()).toMatchSnapshot('clear button');

      resolve();
    });
  });

  function shallowRender(props: Partial<ReactCreatableSelectProps> = {}) {
    return shallow<ReactCreatableSelectProps>(<CreatableLegacy {...props} />);
  }
});

describe('AsyncSelect', () => {
  it('should render correctly', () => {
    expect(shallowRender()).toMatchSnapshot('default');
  });

  function shallowRender(props: Partial<WithInnerRef & ReactAsyncSelectProps> = {}) {
    return shallow<WithInnerRef & ReactAsyncSelectProps>(
      <AsyncSelectLegacy loadOptions={jest.fn()} {...props} />
    );
  }
});
