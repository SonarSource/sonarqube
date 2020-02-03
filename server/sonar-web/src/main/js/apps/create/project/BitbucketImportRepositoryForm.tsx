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
import { uniq, without } from 'lodash';
import * as React from 'react';
import { Link } from 'react-router';
import BoxedGroupAccordion from 'sonar-ui-common/components/controls/BoxedGroupAccordion';
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Radio from 'sonar-ui-common/components/controls/Radio';
import CheckIcon from 'sonar-ui-common/components/icons/CheckIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { colors } from '../../../app/theme';
import { getProjectUrl } from '../../../helpers/urls';
import { BitbucketProject, BitbucketRepository } from '../../../types/alm-integration';

export interface BitbucketImportRepositoryFormProps {
  importing?: boolean;
  onSelectRepository: (repo: BitbucketRepository) => void;
  projects?: BitbucketProject[];
  projectRepositories?: T.Dict<BitbucketRepository[]>;
  selectedRepository?: BitbucketRepository;
}

export default function BitbucketImportRepositoryForm(props: BitbucketImportRepositoryFormProps) {
  const { importing, projects = [], projectRepositories = {}, selectedRepository } = props;
  const [openProjectKeys, setOpenProjectKeys] = React.useState(
    projects.length > 0 ? [projects[0].key] : []
  );

  if (projects.length === 0) {
    return (
      <Alert variant="warning">{translate('onboarding.create_project.no_bbs_projects')}</Alert>
    );
  }

  const allAreExpanded = projects.length === openProjectKeys.length;

  return (
    <div className="create-project-import-bbs">
      <div className="overflow-hidden spacer-bottom">
        <ButtonLink
          className="pull-right"
          onClick={() => setOpenProjectKeys(allAreExpanded ? [] : projects.map(p => p.key))}>
          {allAreExpanded ? translate('collapse_all') : translate('expand_all')}
        </ButtonLink>
      </div>

      {projects.map(project => {
        const isOpen = openProjectKeys.includes(project.key);
        const repositories = projectRepositories[project.key] || [];

        return (
          <BoxedGroupAccordion
            className={classNames({ open: isOpen })}
            key={project.key}
            onClick={() =>
              setOpenProjectKeys(
                isOpen
                  ? without(openProjectKeys, project.key)
                  : uniq([...openProjectKeys, project.key])
              )
            }
            open={isOpen}
            title={<h3>{project.name}</h3>}>
            {isOpen && (
              <div className="display-flex-wrap">
                {repositories.length === 0 && (
                  <Alert variant="warning">
                    {translate('onboarding.create_project.no_bbs_repos')}
                  </Alert>
                )}

                {repositories.map(repo =>
                  repo.sqProjectKey ? (
                    <span
                      className="display-inline-flex-start spacer-right spacer-bottom create-project-import-bbs-repo"
                      key={repo.id}>
                      <CheckIcon className="spacer-right" fill={colors.green} size={14} />
                      <span>
                        <div className="little-spacer-bottom">
                          <strong>
                            <Link to={getProjectUrl(repo.sqProjectKey)}>{repo.name}</Link>
                          </strong>
                        </div>
                        <em>{translate('onboarding.create_project.repository_imported')}</em>
                      </span>
                    </span>
                  ) : (
                    <Radio
                      checked={selectedRepository?.id === repo.id}
                      className={classNames(
                        'display-inline-flex-start spacer-right spacer-bottom create-project-import-bbs-repo overflow-hidden',
                        {
                          disabled: importing,
                          'text-muted': importing,
                          'link-no-underline': importing
                        }
                      )}
                      key={repo.id}
                      onCheck={() => props.onSelectRepository(repo)}
                      value={String(repo.id)}>
                      <strong className="text-ellipsis">{repo.name}</strong>
                    </Radio>
                  )
                )}
              </div>
            )}
          </BoxedGroupAccordion>
        );
      })}
    </div>
  );
}
