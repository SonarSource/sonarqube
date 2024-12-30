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

import { Button } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { SubTitle } from '~design-system';
import { ProjectData, ProjectValidationCard } from '../components/ProjectValidation';

interface Props {
  onAddProject: () => void;
  onChangeProject: (project: ProjectData<number>) => void;
  onRemoveProject: (id?: number) => void;
  projectKeys: string[];
  projects: ProjectData<number>[];
}

export function MonorepoProjectsList({
  projectKeys,
  onAddProject,
  onChangeProject,
  onRemoveProject,
  projects,
}: Readonly<Props>) {
  return (
    <div>
      <SubTitle>
        <FormattedMessage id="onboarding.create_project.monorepo.project_title" />
      </SubTitle>
      <div>
        {projects.map(({ id, key, name }) => (
          <ProjectValidationCard
            className="sw-mt-4"
            initialKey={key}
            initialName={name}
            key={id}
            monorepoSetupProjectKeys={projectKeys}
            onChange={onChangeProject}
            onRemove={() => {
              onRemoveProject(id);
            }}
            projectId={id}
          />
        ))}
      </div>

      <div className="sw-flex sw-justify-end sw-mt-4">
        <Button onClick={onAddProject}>
          <FormattedMessage id="onboarding.create_project.monorepo.add_project" />
        </Button>
      </div>
    </div>
  );
}
