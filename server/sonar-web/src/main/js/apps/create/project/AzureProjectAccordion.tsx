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
import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { colors } from '../../../app/theme';
import Link from '../../../components/common/Link';
import BoxedGroupAccordion from '../../../components/controls/BoxedGroupAccordion';
import ListFooter from '../../../components/controls/ListFooter';
import Radio from '../../../components/controls/Radio';
import CheckIcon from '../../../components/icons/CheckIcon';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl, queryToSearch } from '../../../helpers/urls';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import { CreateProjectModes } from './types';

export interface AzureProjectAccordionProps {
  importing: boolean;
  loading: boolean;
  onOpen: (key: string) => void;
  onSelectRepository: (repository: AzureRepository) => void;
  project: AzureProject;
  repositories?: AzureRepository[];
  searchQuery?: string;
  selectedRepository?: AzureRepository;
  startsOpen: boolean;
}

const PAGE_SIZE = 30;

function highlight(text: string, term?: string, underline = false) {
  if (!term || !text.toLowerCase().includes(term.toLowerCase())) {
    return text;
  }

  // Capture only the first occurence by using a capturing group to get
  // everything after the first occurence
  const [pre, found, post] = text.split(new RegExp(`(${term})(.*)`, 'i'));
  return (
    <>
      {pre}
      <strong className={classNames({ underline })}>{found}</strong>
      {post}
    </>
  );
}

export default function AzureProjectAccordion(props: AzureProjectAccordionProps) {
  const {
    importing,
    loading,
    startsOpen,
    project,
    repositories = [],
    searchQuery,
    selectedRepository,
  } = props;

  const [open, setOpen] = React.useState(startsOpen);
  const handleClick = () => {
    if (!open) {
      props.onOpen(project.name);
    }
    setOpen(!open);
  };

  const [page, setPage] = React.useState(1);
  const limitedRepositories = repositories.slice(0, page * PAGE_SIZE);

  const isSelected = (repo: AzureRepository) =>
    selectedRepository?.projectName === project.name && selectedRepository.name === repo.name;

  return (
    <BoxedGroupAccordion
      className={classNames('big-spacer-bottom', {
        open,
      })}
      onClick={handleClick}
      open={open}
      title={<h3 title={project.description}>{highlight(project.name, searchQuery, true)}</h3>}
    >
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
                        search: queryToSearch({
                          mode: CreateProjectModes.AzureDevOps,
                          resetPat: 1,
                        }),
                      }}
                    >
                      {translate('onboarding.create_project.update_your_token')}
                    </Link>
                  ),
                }}
              />
            </Alert>
          ) : (
            <>
              <div className="display-flex-wrap">
                {limitedRepositories.map((repo) => (
                  <div
                    className="create-project-azdo-repo display-flex-start spacer-bottom padded-right"
                    key={repo.name}
                  >
                    {repo.sqProjectKey ? (
                      <>
                        <CheckIcon className="spacer-right" fill={colors.green} size={14} />
                        <div className="overflow-hidden">
                          <div className="little-spacer-bottom text-ellipsis">
                            <Link to={getProjectUrl(repo.sqProjectKey)} title={repo.sqProjectName}>
                              {highlight(repo.sqProjectName || repo.name, searchQuery)}
                            </Link>
                          </div>
                          <em>{translate('onboarding.create_project.repository_imported')}</em>
                        </div>
                      </>
                    ) : (
                      <Radio
                        checked={isSelected(repo)}
                        className="overflow-hidden"
                        alignLabel={true}
                        disabled={importing}
                        onCheck={() => props.onSelectRepository(repo)}
                        value={repo.name}
                      >
                        <span title={repo.name}>{highlight(repo.name, searchQuery)}</span>
                      </Radio>
                    )}
                  </div>
                ))}
              </div>
              <ListFooter
                count={limitedRepositories.length}
                total={repositories.length}
                loadMore={() => setPage((p) => p + 1)}
              />
            </>
          )}
        </DeferredSpinner>
      )}
    </BoxedGroupAccordion>
  );
}
