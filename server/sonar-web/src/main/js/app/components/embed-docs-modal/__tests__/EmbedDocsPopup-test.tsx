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
import * as React from 'react';
import { shallow } from 'enzyme';
import EmbedDocsPopup from '../EmbedDocsPopup';
import { isSonarCloud } from '../../../../helpers/system';

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn().mockReturnValue(false) }));

const suggestions = [{ link: '#', text: 'foo' }, { link: '#', text: 'bar' }];

it('should display suggestion links', () => {
  const context = {};
  const wrapper = shallow(
    <EmbedDocsPopup
      currentUser={{ isLoggedIn: true }}
      onClose={jest.fn()}
      suggestions={suggestions}
    />,
    {
      context
    }
  );
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should display analyze new project link when user has permission', () => {
  const wrapper = shallow(
    <EmbedDocsPopup
      currentUser={{ isLoggedIn: true, permissions: { global: ['provisioning'] } }}
      onClose={jest.fn()}
      suggestions={suggestions}
    />
  );
  expect(wrapper.find('[data-test="analyze-new-project"]').exists()).toBe(true);
});

it('should not display analyze new project link when user does not have permission', () => {
  const wrapper = shallow(
    <EmbedDocsPopup
      currentUser={{ isLoggedIn: true }}
      onClose={jest.fn()}
      suggestions={suggestions}
    />
  );
  expect(wrapper.find('[data-test="analyze-new-project"]').exists()).toBe(false);
});

it('should display correct links for SonarCloud', () => {
  (isSonarCloud as jest.Mock<any>).mockReturnValueOnce(true);
  const context = {};
  const wrapper = shallow(
    <EmbedDocsPopup
      currentUser={{ isLoggedIn: true }}
      onClose={jest.fn()}
      suggestions={suggestions}
    />,
    {
      context
    }
  );
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});
