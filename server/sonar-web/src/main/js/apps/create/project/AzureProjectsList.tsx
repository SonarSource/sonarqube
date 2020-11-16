/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import AzureProjectAccordion from './AzureProjectAccordion';
import { CreateProjectModes } from './types';

export interface AzureProjectsListProps {
  loadingRepositories: T.Dict<boolean>;
  onOpenProject: (key: string) => void;
  projects?: AzureProject[];
  repositories: T.Dict<AzureRepository[]>;
}

const PAGE_SIZE = 10;

export default function AzureProjectsList(props: AzureProjectsListProps) {
  const { loadingRepositories, projects = [], repositories } = props;

  const [page, setPage] = React.useState(1);

  if (projects.length === 0) {
    return (
      <Alert className="spacer-top" variant="warning">
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.azure.no_projects')}
          id="onboarding.create_project.azure.no_projects"
          values={{
            link: (
              <Link
                to={{
                  pathname: '/projects/create',
                  query: { mode: CreateProjectModes.AzureDevOps, resetPat: 1 }
                }}>
                {translate('onboarding.create_project.update_your_token')}
              </Link>
            )
          }}
        />
      </Alert>
    );
  }

  const filteredProjects = projects.slice(0, page * PAGE_SIZE);

  return (
    <div>
      {filteredProjects.map((p, i) => (
        <AzureProjectAccordion
          key={p.key}
          loading={Boolean(loadingRepositories[p.key])}
          onOpen={props.onOpenProject}
          project={p}
          repositories={repositories[p.key]}
          startsOpen={i === 0}
        />
      ))}

      <ListFooter
        count={filteredProjects.length}
        loadMore={() => setPage(p => p + 1)}
        total={projects.length}
      />
    </div>
  );
}
