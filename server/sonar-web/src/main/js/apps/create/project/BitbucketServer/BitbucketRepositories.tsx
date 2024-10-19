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
import { uniq, without } from 'lodash';
import * as React from 'react';
import { BitbucketProject, BitbucketRepository } from '../../../../types/alm-integration';
import { Dict } from '../../../../types/types';
import { DEFAULT_BBS_PAGE_SIZE } from '../constants';
import BitbucketProjectAccordion from './BitbucketProjectAccordion';

export interface BitbucketRepositoriesProps {
  onImportRepository: (repo: BitbucketRepository) => void;
  projectRepositories: Dict<BitbucketRepository[]>;
  projects: BitbucketProject[];
}

export default function BitbucketRepositories(props: BitbucketRepositoriesProps) {
  const { projects, projectRepositories } = props;

  const [openProjectKeys, setOpenProjectKeys] = React.useState(
    projects.length > 0 ? [projects[0].key] : [],
  );

  const handleClick = (isOpen: boolean, projectKey: string) => {
    setOpenProjectKeys(
      isOpen ? without(openProjectKeys, projectKey) : uniq([...openProjectKeys, projectKey]),
    );
  };

  return (
    <>
      {projects.map((project) => {
        const isOpen = openProjectKeys.includes(project.key);
        const repositories = projectRepositories[project.key] ?? [];

        return (
          <BitbucketProjectAccordion
            key={project.key}
            onClick={() => handleClick(isOpen, project.key)}
            open={isOpen}
            project={project}
            repositories={repositories}
            showingAllRepositories={repositories.length < DEFAULT_BBS_PAGE_SIZE}
            onImportRepository={props.onImportRepository}
          />
        );
      })}
    </>
  );
}
