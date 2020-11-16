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
import * as classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import BoxedGroupAccordion from 'sonar-ui-common/components/controls/BoxedGroupAccordion';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import { CreateProjectModes } from './types';

export interface AzureProjectAccordionProps {
  loading: boolean;
  onOpen: (key: string) => void;
  startsOpen: boolean;
  project: AzureProject;
  repositories?: AzureRepository[];
}

const PAGE_SIZE = 30;

export default function AzureProjectAccordion(props: AzureProjectAccordionProps) {
  const { loading, startsOpen, project, repositories = [] } = props;

  const [open, setOpen] = React.useState(startsOpen);
  const handleClick = () => {
    if (!open) {
      props.onOpen(project.key);
    }
    setOpen(!open);
  };

  const [page, setPage] = React.useState(1);
  const limitedRepositories = repositories.slice(0, page * PAGE_SIZE);

  return (
    <BoxedGroupAccordion
      className={classNames('big-spacer-bottom', {
        open
      })}
      onClick={handleClick}
      open={open}
      title={<h3>{project.name}</h3>}>
      {open && (
        <DeferredSpinner loading={loading}>
          {/* The extra loading guard is to prevent the flash of the Alert */}
          {!loading && repositories.length === 0 ? (
            <Alert variant="warning">
              <FormattedMessage
                defaultMessage={translate('onboarding.create_project.azure.no_repositories')}
                id="onboarding.create_project.azure.no_repositories"
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
          ) : (
            <>
              <div className="display-flex-wrap">
                {limitedRepositories.map(repo => (
                  <div
                    className="abs-width-400 overflow-hidden spacer-top spacer-bottom"
                    key={repo.name}>
                    <strong className="text-ellipsis" title={repo.name}>
                      {repo.name}
                    </strong>
                  </div>
                ))}
              </div>
              <ListFooter
                count={limitedRepositories.length}
                total={repositories.length}
                loadMore={() => setPage(p => p + 1)}
              />
            </>
          )}
        </DeferredSpinner>
      )}
    </BoxedGroupAccordion>
  );
}
