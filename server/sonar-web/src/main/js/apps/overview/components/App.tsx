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

import { Helmet } from 'react-helmet-async';
import { isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { translate } from '../../../helpers/l10n';
import { useCurrentBranchQuery } from '../../../queries/branch';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import BranchOverview from '../branches/BranchOverview';
import PullRequestOverview from '../pullRequests/PullRequestOverview';
import EmptyOverview from './EmptyOverview';

interface AppProps extends WithAvailableFeaturesProps {
  component: Component;
}

export function App(props: AppProps) {
  const { component } = props;
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);
  const { data: branchLike } = useCurrentBranchQuery(component);

  if (isPortfolioLike(component.qualifier) || !branchLike) {
    return null;
  }

  return (
    <>
      <Helmet defer={false} title={translate('overview.page')} />
      {isPullRequest(branchLike) ? (
        <main>
          <Suggestions suggestionGroup="pull_requests" />
          <PullRequestOverview pullRequest={branchLike} component={component} />
        </main>
      ) : (
        <main>
          <Suggestions suggestionGroup="overview" />

          {!component.analysisDate && (
            <EmptyOverview branchLike={branchLike} component={component} />
          )}

          {component.analysisDate && (
            <BranchOverview
              branch={branchLike}
              branchesEnabled={branchSupportEnabled}
              component={component}
            />
          )}
        </main>
      )}
    </>
  );
}

export default withComponentContext(withAvailableFeatures(App));
