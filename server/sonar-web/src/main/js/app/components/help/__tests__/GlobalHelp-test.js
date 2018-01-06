/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import { shallow } from 'enzyme';
import GlobalHelp from '../GlobalHelp';
import { click } from '../../../../helpers/testUtils';

it('switches between tabs', () => {
  const wrapper = shallow(
    <GlobalHelp
      currentUser={{ isLoggedIn: true }}
      onClose={jest.fn()}
      onTutorialSelect={jest.fn()}
    />
  );
  expect(wrapper.find('ShortcutsHelp')).toHaveLength(1);
  clickOnSection(wrapper, 'links');
  expect(wrapper.find('LinksHelp')).toHaveLength(1);
  clickOnSection(wrapper, 'tutorials');
  expect(wrapper.find('TutorialsHelp')).toHaveLength(1);
  clickOnSection(wrapper, 'shortcuts');
  expect(wrapper.find('ShortcutsHelp')).toHaveLength(1);
});

it('does not show tutorials for anonymous', () => {
  expect(
    shallow(
      <GlobalHelp
        currentUser={{ isLoggedIn: false }}
        onClose={jest.fn()}
        onTutorialSelect={jest.fn()}
      />
    )
  ).toMatchSnapshot();
});

it('display special links page for SonarCloud', () => {
  const wrapper = shallow(
    <GlobalHelp
      currentUser={{ isLoggedIn: false }}
      onClose={jest.fn()}
      onTutorialSelect={jest.fn()}
      onSonarCloud={true}
    />
  );
  clickOnSection(wrapper, 'links');
  expect(wrapper.find('LinksHelpSonarCloud')).toHaveLength(1);
});

function clickOnSection(wrapper /*: Object */, section /*: string */) {
  click(wrapper.find(`[data-section="${section}"]`), { currentTarget: { dataset: { section } } });
}
