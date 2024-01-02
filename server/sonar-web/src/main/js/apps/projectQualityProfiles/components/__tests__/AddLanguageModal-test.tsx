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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import Select from '../../../../components/controls/Select';
import SimpleModal from '../../../../components/controls/SimpleModal';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { AddLanguageModal, AddLanguageModalProps } from '../AddLanguageModal';

it('should render correctly', () => {
  expect(diveIntoSimpleModal(shallowRender())).toMatchSnapshot('default');
});

it('should correctly handle changes', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });

  const langSelect = getLanguageSelect(wrapper);
  let profileSelect = getProfileSelect(wrapper);

  // Language select should only have 2; JS is not available. Profile Select
  // should have none, as no language is selected yet.
  expect(langSelect.props().options).toHaveLength(2);
  expect(profileSelect.props().options).toHaveLength(0);

  // Choose CSS.
  const langChange = langSelect.props().onChange;

  expect(langChange).toBeDefined();

  langChange!({ value: 'css' });

  // Should now show 2 available profiles.
  profileSelect = getProfileSelect(wrapper);
  expect(profileSelect.props().options).toHaveLength(2);
  expect(profileSelect.props().options).toEqual(
    expect.arrayContaining([expect.objectContaining({ isDisabled: true })])
  );

  // Choose 1 profile.
  const profileChange = profileSelect.props().onChange;

  expect(profileChange).toBeDefined();

  profileChange!({ value: 'css2' });

  submitSimpleModal(wrapper);
  expect(onSubmit).toHaveBeenLastCalledWith('css2');
});

function diveIntoSimpleModal(wrapper: ShallowWrapper) {
  return wrapper.find(SimpleModal).dive().children();
}

function getLanguageSelect(wrapper: ShallowWrapper) {
  return diveIntoSimpleModal(wrapper).find(Select).at(0);
}

function getProfileSelect(wrapper: ShallowWrapper) {
  return diveIntoSimpleModal(wrapper).find(Select).at(1);
}

function submitSimpleModal(wrapper: ShallowWrapper) {
  wrapper.find(SimpleModal).props().onSubmit();
}

function shallowRender(props: Partial<AddLanguageModalProps> = {}) {
  return shallow<AddLanguageModalProps>(
    <AddLanguageModal
      languages={{
        css: { key: 'css', name: 'CSS' },
        ts: { key: 'ts', name: 'TS' },
        js: { key: 'js', name: 'JS' },
      }}
      onClose={jest.fn()}
      onSubmit={jest.fn()}
      profilesByLanguage={{
        css: [
          mockQualityProfile({ key: 'css', name: 'CSS', activeRuleCount: 0 }),
          mockQualityProfile({ key: 'css2', name: 'CSS 2' }),
        ],
        ts: [mockQualityProfile({ key: 'ts', name: 'TS' })],
        js: [mockQualityProfile({ key: 'js', name: 'JS' })],
      }}
      unavailableLanguages={['js']}
      {...props}
    />
  );
}
