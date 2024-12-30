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

import styled from '@emotion/styled';
import { Spinner, Text } from '@sonarsource/echoes-react';
import { useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { InputSearch, LargeCenteredLayout } from '~design-system';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import DocumentationLink from '../../components/common/DocumentationLink';
import ListFooter from '../../components/controls/ListFooter';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { useCurrentBranchQuery } from '../../queries/branch';
import { useDependenciesQuery } from '../../queries/dependencies';
import { withRouter } from '../../sonar-aligned/components/hoc/withRouter';
import { getBranchLikeQuery } from '../../sonar-aligned/helpers/branch-like';
import { BranchLikeParameters } from '../../sonar-aligned/types/branch-like';
import { Component } from '../../types/types';
import DependencyListItem from './components/DependencyListItem';

const SEARCH_MIN_LENGTH = 3;

interface Props {
  component: Component;
}

function App(props: Readonly<Props>) {
  const { component } = props;
  const { data: branchLike } = useCurrentBranchQuery(component);

  const [search, setSearch] = useState('');

  const {
    data: { pages, pageParams } = {
      pages: [],
      pageParams: [],
    },
    isLoading,
    fetchNextPage,
    isFetchingNextPage,
  } = useDependenciesQuery({
    projectKey: component.key,
    q: search,
    branchParameters: getBranchLikeQuery(branchLike) as BranchLikeParameters,
  });

  const listName = search ? 'dependencies.list.name_search.title' : 'dependencies.list.title';

  const { resultsExist, allDependencies, totalDependencies, itemCount } = useMemo(() => {
    // general paging information
    const { total, pageSize } = pages[0]?.page ?? { total: 0, pageSize: 0 };

    const resultsExist = total > 0 || search.length >= SEARCH_MIN_LENGTH;

    const allDependencies = pages.flatMap((page) => page.dependencies);
    const totalDependencies = pages[0]?.page.total ?? 0;

    const itemCount = pageSize * pages.length > total ? total : pageSize * pages.length;

    return { resultsExist, allDependencies, totalDependencies, itemCount };
  }, [pages, search]);

  return (
    <LargeCenteredLayout className="sw-py-8 sw-typo-lg sw-h-full" id="dependencies-page">
      <Helmet defer={false} title={translate('dependencies.page')} />
      <main className="sw-relative sw-flex-1 sw-min-w-0 sw-h-full">
        {resultsExist && (
          <div className="sw-flex sw-justify-between">
            <InputSearch
              className="sw-mb-4"
              searchInputAriaLabel={translate('search.search_for_dependencies')}
              minLength={SEARCH_MIN_LENGTH}
              value={search}
              onChange={(value) => setSearch(value.toLowerCase())}
              placeholder={translate('search.search_for_dependencies')}
              size="large"
            />
          </div>
        )}

        <Spinner isLoading={isLoading}>
          {!resultsExist && <EmptyState />}
          {resultsExist && (
            <div className="sw-overflow-auto">
              <Text>
                <FormattedMessage
                  id={listName}
                  defaultMessage={translate(listName)}
                  values={{
                    count: allDependencies.length,
                  }}
                />
              </Text>
              <ul className="sw-py-4">
                {allDependencies.map((dependency) => (
                  <li key={dependency.key}>
                    <DependencyListItem dependency={dependency} />
                  </li>
                ))}
              </ul>
              {pageParams.length > 0 && (
                <ListFooter
                  className="sw-mb-4"
                  count={itemCount}
                  loadMore={() => fetchNextPage()}
                  loading={isFetchingNextPage}
                  total={totalDependencies}
                />
              )}
            </div>
          )}
        </Spinner>
      </main>
    </LargeCenteredLayout>
  );
}

function EmptyState() {
  return (
    <CenteredDiv className="sw-w-[450px] sw-mt-[185px] sw-flex sw-flex-col sw-gap-4 sw-text-center sw-mx-auto">
      <Text isHighlighted>{translate('dependencies.empty_state.title')}</Text>
      <Text>{translate('dependencies.empty_state.body')}</Text>
      <Text>
        <DocumentationLink
          to={DocLink.Dependencies}
          shouldOpenInNewTab
          className="sw-font-semibold"
        >
          {translate('dependencies.empty_state.link_text')}
        </DocumentationLink>
      </Text>
    </CenteredDiv>
  );
}

const CenteredDiv = styled('div')`
  height: 50vh;
`;

export default withRouter(withComponentContext(App));
