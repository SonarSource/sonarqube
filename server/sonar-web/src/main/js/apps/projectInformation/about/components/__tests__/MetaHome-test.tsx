/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { setHomePage } from '../../../../../api/users';
import { CurrentUserContext } from '../../../../../app/components/current-user/CurrentUserContext';
import { mockLoggedInUser } from '../../../../../helpers/testMocks';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../../helpers/testSelector';
import { LoggedInUser, isLoggedIn } from '../../../../../types/users';
import MetaHome from '../MetaHome';

jest.mock('../../../../../api/users', () => ({
  setHomePage: jest.fn().mockImplementation(() => Promise.resolve()),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

const ui = {
  checkbox: byRole('checkbox', { name: /info.make_home.label/ }),
};

it('should update homepage for project', async () => {
  const user = userEvent.setup();
  renderMetaHome(
    mockLoggedInUser({
      homepage: { component: 'test-project', type: 'PROJECT', branch: undefined },
    })
  );

  expect(await ui.checkbox.find()).toBeInTheDocument();
  expect(ui.checkbox.get()).not.toBeChecked();
  await act(() => user.click(ui.checkbox.get()));
  expect(ui.checkbox.get()).toBeChecked();
  expect(jest.mocked(setHomePage)).toHaveBeenCalledWith({
    component: 'my-project',
    type: 'PROJECT',
    branch: undefined,
  });
  await act(() => user.click(ui.checkbox.get()));
  expect(jest.mocked(setHomePage)).toHaveBeenCalledWith({ type: 'PROJECTS' });
  expect(await ui.checkbox.find()).not.toBeChecked();
});

it('should update homepage for application', async () => {
  const user = userEvent.setup();
  renderMetaHome(
    mockLoggedInUser({
      homepage: { component: 'test-project', type: 'PROJECT', branch: undefined },
    }),
    'test',
    true
  );

  expect(await ui.checkbox.find()).toBeInTheDocument();
  expect(ui.checkbox.get()).not.toBeChecked();
  await act(() => user.click(ui.checkbox.get()));
  expect(ui.checkbox.get()).toBeChecked();
  expect(jest.mocked(setHomePage)).toHaveBeenCalledWith({
    component: 'test',
    type: 'APPLICATION',
    branch: undefined,
  });
});

function TestComponent({
  user,
  componentKey,
  isApp,
}: {
  user: LoggedInUser;
  componentKey: string;
  isApp?: boolean;
}) {
  const [currentUser, setCurrentUser] = React.useState(user);
  return (
    <CurrentUserContext.Provider
      value={{
        currentUser,
        updateCurrentUserHomepage: (homepage) => {
          setCurrentUser({ ...currentUser, homepage });
        },
        updateDismissedNotices: () => {},
      }}
    >
      <CurrentUserContext.Consumer>
        {({ currentUser }) =>
          isLoggedIn(currentUser) ? (
            <MetaHome componentKey={componentKey} currentUser={currentUser} isApp={isApp} />
          ) : null
        }
      </CurrentUserContext.Consumer>
    </CurrentUserContext.Provider>
  );
}

function renderMetaHome(
  currentUser: LoggedInUser,
  componentKey: string = 'my-project',
  isApp?: boolean
) {
  return renderComponent(
    <TestComponent user={currentUser} componentKey={componentKey} isApp={isApp} />
  );
}
