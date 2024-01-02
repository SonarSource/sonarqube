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
import { AlmKeys } from '../../../../types/alm-settings';
import Step from '../../components/Step';
import SelectAlmStep, { SelectAlmStepProps } from '../SelectAlmStep';

jest.mock('../../../../helpers/l10n', () => ({
  hasMessage: (_a: string, k: string, _b: string) => k === AlmKeys.BitbucketCloud,
  translate: (...k: string[]) => k.join('.'),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender().find(Step).props().renderForm()).toMatchSnapshot('form, default');
  expect(shallowRender({ alm: AlmKeys.Azure }).find(Step).props().renderForm()).toMatchSnapshot(
    'form, with alm'
  );
  expect(shallowRender().find(Step).props().renderResult!()).toBeUndefined();
  expect(
    shallowRender({ alm: AlmKeys.BitbucketCloud }).find(Step).props().renderResult!()
  ).toMatchSnapshot('result, with alm');
});

function shallowRender(props: Partial<SelectAlmStepProps> = {}) {
  return shallow<SelectAlmStepProps>(
    <SelectAlmStep onCheck={jest.fn()} onOpen={jest.fn()} open={true} {...props} />
  );
}
