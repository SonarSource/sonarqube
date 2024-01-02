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
import DisableableSelectOption from '../../../../components/common/DisableableSelectOption';
import { mockProfileOption } from '../../../../helpers/mocks/quality-profiles';
import LanguageProfileSelectOption, {
  LanguageProfileSelectOptionProps,
  ProfileOption,
} from '../LanguageProfileSelectOption';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
});

describe('tooltip', () => {
  it('should render correctly', () => {
    expect(renderTooltipOverly()).toMatchSnapshot('default');
    expect(renderTooltipOverly({ label: undefined })).toMatchSnapshot('no link');
  });
});

function renderTooltipOverly(overrides: Partial<ProfileOption> = {}) {
  const wrapper = shallowRender(overrides);
  const { disableTooltipOverlay } = wrapper.find(DisableableSelectOption).props();
  return disableTooltipOverlay();
}

function shallowRender(overrides: Partial<ProfileOption> = {}) {
  // satisfy the required props that the option actually gets from the select
  const optionProps = {} as LanguageProfileSelectOptionProps;
  return shallow(
    <LanguageProfileSelectOption {...optionProps} data={{ ...mockProfileOption(), ...overrides }} />
  );
}
