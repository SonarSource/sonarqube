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
import { KeyboardKeys } from '../../../helpers/keycodes';
import { mockIssue } from '../../../helpers/testMocks';
import { keydown } from '../../../helpers/testUtils';
import Issue from '../Issue';

it('should render issues correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should call the proper function with the proper props when pressing shortcuts (FAMICT)', () => {
  const onPopupToggle = jest.fn();
  const onCheck = jest.fn();
  const issue = mockIssue(false, {
    comments: [
      {
        key: '1',
        htmlText: 'My comment',
        markdown: 'My comment',
        updatable: false,
        createdAt: '2017-07-05T09:33:29+0200',
        author: 'admin',
        authorLogin: 'admin',
        authorName: 'Admin',
        authorAvatar: 'admin-avatar',
        authorActive: true,
      },
    ],
    actions: ['assign'],
  });

  shallowRender({ onPopupToggle, issue, onCheck });
  keydown({ key: KeyboardKeys.KeyF, metaKey: true });
  expect(onPopupToggle).not.toHaveBeenCalledWith(issue.key, 'transition', undefined);

  keydown({ key: KeyboardKeys.KeyF });
  expect(onPopupToggle).toHaveBeenCalledWith(issue.key, 'transition', undefined);

  keydown({ key: KeyboardKeys.KeyA });
  expect(onPopupToggle).toHaveBeenCalledWith(issue.key, 'assign', undefined);
  keydown({ key: KeyboardKeys.Escape });

  keydown({ key: KeyboardKeys.KeyM });
  expect(onPopupToggle).toHaveBeenCalledWith(issue.key, 'assign', false);

  keydown({ key: KeyboardKeys.KeyI });
  expect(onPopupToggle).toHaveBeenCalledWith(issue.key, 'set-severity', undefined);

  keydown({ key: KeyboardKeys.KeyC, metaKey: true });
  expect(onPopupToggle).not.toHaveBeenCalledWith(issue.key, 'comment', undefined);

  keydown({ key: KeyboardKeys.KeyC });
  expect(onPopupToggle).toHaveBeenCalledWith(issue.key, 'comment', undefined);
  keydown({ key: KeyboardKeys.Escape });

  keydown({ key: KeyboardKeys.KeyT });
  expect(onPopupToggle).toHaveBeenCalledWith(issue.key, 'edit-tags', undefined);

  keydown({ key: KeyboardKeys.Space });
  expect(onCheck).toHaveBeenCalledWith(issue.key);
});

function shallowRender(props: Partial<Issue['props']> = {}) {
  return shallow<Issue>(
    <Issue
      displayLocationsCount={true}
      displayLocationsLink={false}
      issue={mockIssue(false, {
        comments: [
          {
            key: '1',
            htmlText: 'My comment',
            markdown: 'My comment',
            updatable: false,
            createdAt: '2017-07-05T09:33:29+0200',
            author: 'admin',
            authorLogin: 'admin',
            authorName: 'Admin',
            authorAvatar: 'admin-avatar',
            authorActive: true,
          },
        ],
      })}
      onChange={jest.fn()}
      onCheck={jest.fn()}
      onClick={jest.fn()}
      onFilter={jest.fn()}
      onPopupToggle={jest.fn()}
      selected={true}
      {...props}
    />
  );
}
