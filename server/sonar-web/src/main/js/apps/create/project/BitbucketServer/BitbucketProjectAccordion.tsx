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
import { Accordion, FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { queryToSearch } from '~sonar-aligned/helpers/urls';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { BitbucketProject, BitbucketRepository } from '../../../../types/alm-integration';
import AlmRepoItem from '../components/AlmRepoItem';
import { CreateProjectModes } from '../types';

export interface BitbucketProjectAccordionProps {
  onClick?: () => void;
  onImportRepository: (repository: BitbucketRepository) => void;
  open: boolean;
  project?: BitbucketProject;
  repositories: BitbucketRepository[];
  showingAllRepositories: boolean;
}

export default function BitbucketProjectAccordion(props: BitbucketProjectAccordionProps) {
  const { open, project, repositories, showingAllRepositories } = props;

  const repositoryCount = repositories.length;

  const title = project?.name ?? translate('search_results');

  return (
    <Accordion
      className="sw-mb-6"
      onClick={
        props.onClick
          ? props.onClick
          : () => {
              /* noop */
            }
      }
      open={open}
      header={title}
    >
      {open && (
        <>
          <div className="sw-mb-4">
            {repositoryCount === 0 && (
              <FlagMessage variant="warning">
                <span>
                  <FormattedMessage
                    defaultMessage={translate('onboarding.create_project.no_bbs_repos')}
                    id="onboarding.create_project.no_bbs_repos"
                    values={{
                      link: (
                        <Link
                          to={{
                            pathname: '/projects/create',
                            search: queryToSearch({
                              mode: CreateProjectModes.BitbucketServer,
                              resetPat: 1,
                            }),
                          }}
                        >
                          {translate('onboarding.create_project.update_your_token')}
                        </Link>
                      ),
                    }}
                  />
                </span>
              </FlagMessage>
            )}

            <ul className="sw-flex sw-flex-col sw-gap-3">
              {repositories.map((r) => (
                <AlmRepoItem
                  key={r.name}
                  almKey={r.name}
                  almIconSrc={`${getBaseUrl()}/images/alm/bitbucket.svg`}
                  sqProjectKey={r.sqProjectKey}
                  onImport={() => props.onImportRepository(r)}
                  primaryTextNode={<span>{r.name}</span>}
                />
              ))}
            </ul>
          </div>

          {!showingAllRepositories && repositoryCount > 0 && (
            <FlagMessage variant="warning">
              {translateWithParameters(
                'onboarding.create_project.only_showing_X_first_repos',
                repositoryCount,
              )}
            </FlagMessage>
          )}
        </>
      )}
    </Accordion>
  );
}
