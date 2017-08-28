/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { parseIssueFromResponse } from '../issues';

it('should populate comments data', () => {
  const users = [
    {
      active: true,
      avatar: 'c1244e6857f7be3dc4549d9e9d51c631',
      login: 'admin',
      name: 'Admin Admin'
    }
  ];
  const issue = {
    comments: [
      {
        createdAt: '2017-04-11T10:38:09+0200',
        htmlText: 'comment!',
        key: 'AVtcKbZkQmGLa7yW8J71',
        login: 'admin',
        markdown: 'comment!',
        updatable: true
      }
    ]
  } as any;
  expect(parseIssueFromResponse(issue, undefined, users, undefined).comments).toEqual([
    {
      author: 'admin',
      authorActive: true,
      authorAvatar: 'c1244e6857f7be3dc4549d9e9d51c631',
      authorLogin: 'admin',
      authorName: 'Admin Admin',
      createdAt: '2017-04-11T10:38:09+0200',
      htmlText: 'comment!',
      key: 'AVtcKbZkQmGLa7yW8J71',
      login: undefined,
      markdown: 'comment!',
      updatable: true
    }
  ]);
});
