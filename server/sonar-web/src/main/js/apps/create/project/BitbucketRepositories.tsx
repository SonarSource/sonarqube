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
import { ButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import {
  BitbucketProject,
  BitbucketProjectRepositories,
  BitbucketRepository,
} from '../../../types/alm-integration';
import BitbucketProjectAccordion from './BitbucketProjectAccordion';

export interface BitbucketRepositoriesProps {
  disableRepositories: boolean;
  onSelectRepository: (repo: BitbucketRepository) => void;
  projects: BitbucketProject[];
  projectRepositories: BitbucketProjectRepositories;
  selectedRepository?: BitbucketRepository;
}

export default function BitbucketRepositories(props: BitbucketRepositoriesProps) {
  const { disableRepositories, projects, projectRepositories, selectedRepository } = props;

  const [openProjectKeys, setOpenProjectKeys] = React.useState(
    projects.length > 0 ? [projects[0].key] : []
  );

  const allAreExpanded = projects.length <= openProjectKeys.length;

  const handleClick = (isOpen: boolean, projectKey: string) => {
    setOpenProjectKeys(
      isOpen ? without(openProjectKeys, projectKey) : uniq([...openProjectKeys, projectKey])
    );
  };

  return (
    <>
      <div className="overflow-hidden spacer-bottom">
        <ButtonLink
          className="pull-right"
          onClick={() => setOpenProjectKeys(allAreExpanded ? [] : projects.map((p) => p.key))}
        >
          {allAreExpanded ? translate('collapse_all') : translate('expand_all')}
        </ButtonLink>
      </div>

      {projects.map((project) => {
        const isOpen = openProjectKeys.includes(project.key);
        const { allShown, repositories = [] } = projectRepositories[project.key] || {};

        return (
          <BitbucketProjectAccordion
            disableRepositories={disableRepositories}
            key={project.key}
            onClick={() => handleClick(isOpen, project.key)}
            onSelectRepository={props.onSelectRepository}
            open={isOpen}
            project={project}
            repositories={repositories}
            selectedRepository={selectedRepository}
            showingAllRepositories={allShown}
          />
        );
      })}
    </>
  );
}
