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
import HomePageSelect from '../HomePageSelect';
import { setHomePage } from '../../../api/users';
import { HomePageType, HomePage, LoggedInUser } from '../../../app/types';
import { click } from '../../../helpers/testUtils';
import rootReducer, { getCurrentUser } from '../../../store/rootReducer';
import configureStore from '../../../store/utils/configureStore';

jest.mock('../../../api/users', () => ({
  setHomePage: jest.fn(() => Promise.resolve())
}));

const homepage: HomePage = { type: HomePageType.Projects };

it('should render unchecked', () => {
  const store = configureStore(rootReducer, { users: { currentUser: { isLoggedIn: true } } });
  expect(getWrapper(homepage, store)).toMatchSnapshot();
});

it('should render checked', () => {
  const store = configureStore(rootReducer, {
    users: { currentUser: { isLoggedIn: true, homepage } }
  });
  expect(getWrapper(homepage, store)).toMatchSnapshot();
});

it('should set new home page', async () => {
  const store = configureStore(rootReducer, { users: { currentUser: { isLoggedIn: true } } });
  const wrapper = getWrapper(homepage, store);
  click(wrapper.find('a'));
  await new Promise(setImmediate);
  const currentUser = getCurrentUser(store.getState()) as LoggedInUser;
  expect(currentUser.homepage).toEqual(homepage);
  expect(setHomePage).toBeCalledWith(homepage);
});

it('should not render for anonymous', () => {
  const store = configureStore(rootReducer, { users: { currentUser: { isLoggedIn: false } } });
  expect(getWrapper(homepage, store).type()).toBeNull();
});

function getWrapper(currentPage: HomePage, store: any) {
  return shallow(<HomePageSelect currentPage={currentPage} />, {
    context: { store }
  }).dive();
}
