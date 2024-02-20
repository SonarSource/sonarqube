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
import { FlagMessage, InputSearch, LightPrimary, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import ListFooter from '../../../../components/controls/ListFooter';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { queryToSearch } from '../../../../helpers/urls';
import { BitbucketCloudRepository } from '../../../../types/alm-integration';
import AlmRepoItem from '../components/AlmRepoItem';
import { BITBUCKET_CLOUD_PROJECTS_PAGESIZE } from '../constants';
import { CreateProjectModes } from '../types';

export interface BitbucketCloudSearchFormProps {
  isLastPage: boolean;
  loadingMore: boolean;
  onImport: (repositorySlug: string) => void;
  onLoadMore: () => void;
  onSearch: (searchQuery: string) => void;
  repositories?: BitbucketCloudRepository[];
  searching: boolean;
  searchQuery: string;
}

function getRepositoryUrl(workspace: string, slug: string) {
  return `https://bitbucket.org/${workspace}/${slug}`;
}

export default function BitbucketCloudSearchForm(props: BitbucketCloudSearchFormProps) {
  const { isLastPage, loadingMore, repositories = [], searching, searchQuery } = props;

  if (repositories.length === 0 && searchQuery.length === 0 && !searching) {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        <span>
          <FormattedMessage
            defaultMessage={translate('onboarding.create_project.bitbucketcloud.no_projects')}
            id="onboarding.create_project.bitbucketcloud.no_projects"
            values={{
              link: (
                <Link
                  to={{
                    pathname: '/projects/create',
                    search: queryToSearch({ mode: CreateProjectModes.BitbucketCloud, resetPat: 1 }),
                  }}
                >
                  {translate('onboarding.create_project.update_your_token')}
                </Link>
              ),
            }}
          />
        </span>
      </FlagMessage>
    );
  }

  return (
    <div>
      <div className="sw-flex sw-items-center sw-mb-6 sw-w-abs-400">
        <InputSearch
          loading={searching}
          minLength={3}
          onChange={props.onSearch}
          placeholder={translate('onboarding.create_project.search_prompt')}
          size="full"
          value={searchQuery}
        />
      </div>

      {repositories.length === 0 ? (
        <div className="sw-py-6 sw-px-2">
          <LightPrimary className="sw-body-sm">{translate('no_results')}</LightPrimary>
        </div>
      ) : (
        <ul className="sw-flex sw-flex-col sw-gap-3">
          {repositories.map((r) => (
            <AlmRepoItem
              key={r.uuid}
              almKey={r.slug}
              almUrl={getRepositoryUrl(r.workspace, r.slug)}
              almUrlText={translate('onboarding.create_project.bitbucketcloud.link')}
              almIconSrc={`${getBaseUrl()}/images/alm/bitbucket.svg`}
              sqProjectKey={r.sqProjectKey}
              onImport={props.onImport}
              primaryTextNode={<span title={r.name}>{r.name}</span>}
              secondaryTextNode={<span title={r.projectKey}>{r.projectKey}</span>}
            />
          ))}
        </ul>
      )}

      <ListFooter
        className="sw-mb-10"
        count={repositories.length}
        // we don't know the total, so only provide when we've reached the last page
        total={isLastPage ? repositories.length : undefined}
        pageSize={BITBUCKET_CLOUD_PROJECTS_PAGESIZE}
        loadMore={props.onLoadMore}
        loading={loadingMore}
      />
    </div>
  );
}
