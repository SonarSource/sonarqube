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
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { whenLoggedIn } from '../whenLoggedIn';

jest.mock('../../../helpers/handleRequiredAuthentication', () => jest.fn());

class X extends React.Component {
  render() {
    return <div />;
  }
}

const UnderTest = whenLoggedIn(X);

it('should render for logged in user', () => {
  expect(getRenderedType(shallowRender())).toBe(X);
});

it('should not render for anonymous user', () => {
  const wrapper = shallowRender(false);
  expect(getRenderedType(wrapper)).toBeNull();
  expect(handleRequiredAuthentication).toHaveBeenCalled();
});

function getRenderedType(wrapper: ShallowWrapper) {
  return wrapper.dive().dive().dive().type();
}

function shallowRender(isLoggedIn = true) {
  return shallow(
    <CurrentUserContext.Provider
      value={{
        currentUser: { isLoggedIn, dismissedNotices: {} },
        updateCurrentUserHomepage: () => {},
        updateDismissedNotices: () => {},
      }}
    >
      <UnderTest />
    </CurrentUserContext.Provider>
  );
}
