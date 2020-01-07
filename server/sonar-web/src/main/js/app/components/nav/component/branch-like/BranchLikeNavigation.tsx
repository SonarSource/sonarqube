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
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import { withAppState } from '../../../../../components/hoc/withAppState';
import { BranchLike } from '../../../../../types/branch-like';
import './BranchLikeNavigation.css';
import CurrentBranchLike from './CurrentBranchLike';
import Menu from './Menu';

export interface BranchLikeNavigationProps {
  appState: Pick<T.AppState, 'branchesEnabled'>;
  branchLikes: BranchLike[];
  component: T.Component;
  currentBranchLike: BranchLike;
}

export function BranchLikeNavigation(props: BranchLikeNavigationProps) {
  const {
    appState: { branchesEnabled },
    branchLikes,
    component,
    component: { configuration },
    currentBranchLike
  } = props;

  const [isMenuOpen, setIsMenuOpen] = React.useState(false);

  const canAdminComponent = configuration && configuration.showSettings;
  const hasManyBranches = branchLikes.length >= 2;
  const isMenuEnabled = branchesEnabled && hasManyBranches;

  const currentBranchLikeElement = (
    <CurrentBranchLike
      branchesEnabled={Boolean(branchesEnabled)}
      component={component}
      currentBranchLike={currentBranchLike}
      hasManyBranches={hasManyBranches}
    />
  );

  return (
    <span
      className={classNames('big-spacer-left flex-0 branch-like-navigation-toggler-container', {
        dropdown: isMenuEnabled
      })}>
      {isMenuEnabled ? (
        <Toggler
          onRequestClose={() => setIsMenuOpen(false)}
          open={isMenuOpen}
          overlay={
            <Menu
              branchLikes={branchLikes}
              canAdminComponent={canAdminComponent}
              component={component}
              currentBranchLike={currentBranchLike}
              onClose={() => setIsMenuOpen(false)}
            />
          }>
          <a
            className="link-base-color link-no-underline"
            href="#"
            onClick={() => setIsMenuOpen(!isMenuOpen)}>
            {currentBranchLikeElement}
          </a>
        </Toggler>
      ) : (
        currentBranchLikeElement
      )}
    </span>
  );
}

export default withAppState(React.memo(BranchLikeNavigation));
