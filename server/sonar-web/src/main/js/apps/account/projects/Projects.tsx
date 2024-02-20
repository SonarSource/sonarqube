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
import * as React from 'react';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
import { MyProject } from '../../../types/types';
import ProjectCard from './ProjectCard';

interface Props {
  loading: boolean;
  loadMore: () => void;
  projects: MyProject[];
  total?: number;
}

export default function Projects(props: Readonly<Props>) {
  const { projects } = props;

  return (
    <div id="account-projects">
      <div className="sw-mt-8">
        {projects.length === 0
          ? translate('my_account.projects.no_results')
          : translate('my_account.projects.description')}
      </div>

      {projects.length > 0 && (
        <>
          <ul className="sw-mt-4 sw-flex sw-flex-col sw-gap-4">
            {projects.map((project) => (
              <li key={project.key}>
                <ProjectCard project={project} />
              </li>
            ))}
          </ul>

          <ListFooter
            count={projects.length}
            loadMore={props.loadMore}
            loading={props.loading}
            ready={!props.loading}
            total={props.total ?? 0}
          />
        </>
      )}
    </div>
  );
}
