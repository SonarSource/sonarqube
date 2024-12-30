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

import { cloneDeep } from 'lodash';
import { ProjectLink } from '../../types/types';
import { createLink, deleteLink, getProjectLinks } from '../projectLinks';

jest.mock('../projectLinks');

export default class ProjectLinksServiceMock {
  projectLinks: ProjectLink[] = [];
  idCounter: number = 0;

  constructor() {
    jest.mocked(getProjectLinks).mockImplementation(this.handleGetProjectLinks);
    jest.mocked(createLink).mockImplementation(this.handleCreateLink);
    jest.mocked(deleteLink).mockImplementation(this.handleDeleteLink);
  }

  handleGetProjectLinks = () => {
    return this.reply(this.projectLinks);
  };

  handleCreateLink = ({ name, url }: { name: string; url: string }) => {
    const link = {
      id: `id${this.idCounter++}`,
      name,
      type: name,
      url,
    };
    this.projectLinks.push(link);

    return this.reply(link);
  };

  handleDeleteLink = (id: string) => {
    this.projectLinks.filter((link) => link.id !== id);

    return this.reply(undefined);
  };

  reset = () => {
    this.projectLinks = [];
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
