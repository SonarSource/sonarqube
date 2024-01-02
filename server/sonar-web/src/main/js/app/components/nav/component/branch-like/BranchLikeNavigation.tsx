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
import { ButtonPlain } from '../../../../../components/controls/buttons';
import Toggler from '../../../../../components/controls/Toggler';
import { ProjectAlmBindingResponse } from '../../../../../types/alm-settings';
import { BranchLike } from '../../../../../types/branch-like';
import { Feature } from '../../../../../types/features';
import { Component } from '../../../../../types/types';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../available-features/withAvailableFeatures';
import './BranchLikeNavigation.css';
import CurrentBranchLike from './CurrentBranchLike';
import Menu from './Menu';

export interface BranchLikeNavigationProps extends WithAvailableFeaturesProps {
  branchLikes: BranchLike[];
  component: Component;
  currentBranchLike: BranchLike;
  projectBinding?: ProjectAlmBindingResponse;
}

export function BranchLikeNavigation(props: BranchLikeNavigationProps) {
  const {
    branchLikes,
    component,
    component: { configuration },
    currentBranchLike,
    projectBinding,
  } = props;

  const [isMenuOpen, setIsMenuOpen] = React.useState(false);
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);

  const canAdminComponent = configuration && configuration.showSettings;
  const hasManyBranches = branchLikes.length >= 2;
  const isMenuEnabled = branchSupportEnabled && hasManyBranches;

  const currentBranchLikeElement = (
    <CurrentBranchLike
      branchesEnabled={branchSupportEnabled}
      component={component}
      currentBranchLike={currentBranchLike}
      hasManyBranches={hasManyBranches}
      projectBinding={projectBinding}
    />
  );

  return (
    <span
      className={classNames(
        'big-spacer-left flex-0 branch-like-navigation-toggler-container display-flex-center',
        {
          dropdown: isMenuEnabled,
        }
      )}
    >
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
          }
        >
          <ButtonPlain
            className={classNames('branch-like-navigation-toggler', { open: isMenuOpen })}
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            aria-expanded={isMenuOpen}
            aria-haspopup="menu"
          >
            {currentBranchLikeElement}
          </ButtonPlain>
        </Toggler>
      ) : (
        currentBranchLikeElement
      )}
    </span>
  );
}

export default withAvailableFeatures(React.memo(BranchLikeNavigation));
