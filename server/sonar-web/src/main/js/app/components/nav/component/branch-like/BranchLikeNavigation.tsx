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
import { ButtonSecondary, Popup, PopupPlacement, PopupZLevel } from 'design-system';
import * as React from 'react';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import EscKeydownHandler from '../../../../../components/controls/EscKeydownHandler';
import FocusOutHandler from '../../../../../components/controls/FocusOutHandler';
import OutsideClickHandler from '../../../../../components/controls/OutsideClickHandler';
import { useBranchesQuery } from '../../../../../queries/branch';
import { Feature } from '../../../../../types/features';
import { Component } from '../../../../../types/types';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../available-features/withAvailableFeatures';
import BranchHelpTooltip from './BranchHelpTooltip';
import CurrentBranchLike from './CurrentBranchLike';
import Menu from './Menu';
import PRLink from './PRLink';

export interface BranchLikeNavigationProps extends WithAvailableFeaturesProps {
  component: Component;
}

export function BranchLikeNavigation(props: BranchLikeNavigationProps) {
  const {
    component,
    component: { configuration },
  } = props;

  const { data: { branchLikes, branchLike: currentBranchLike } = { branchLikes: [] } } =
    useBranchesQuery(component);
  const [isMenuOpen, setIsMenuOpen] = React.useState(false);

  if (currentBranchLike === undefined) {
    return null;
  }

  const isApplication = component.qualifier === ComponentQualifier.Application;

  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);
  const canAdminComponent = configuration?.showSettings;
  const hasManyBranches = branchLikes.length >= 2;
  const isMenuEnabled = branchSupportEnabled && hasManyBranches;

  const currentBranchLikeElement = <CurrentBranchLike currentBranchLike={currentBranchLike} />;

  const handleOutsideClick = () => {
    setIsMenuOpen(false);
  };

  return (
    <>
      <SlashSeparator className=" sw-mx-2" />
      <div className="sw-flex sw-items-center it__branch-like-navigation-toggler-container">
        <Popup
          allowResizing
          overlay={
            isMenuOpen && (
              <FocusOutHandler onFocusOut={handleOutsideClick}>
                <EscKeydownHandler onKeydown={handleOutsideClick}>
                  <OutsideClickHandler onClickOutside={handleOutsideClick}>
                    <Menu
                      branchLikes={branchLikes}
                      canAdminComponent={canAdminComponent}
                      component={component}
                      currentBranchLike={currentBranchLike}
                      onClose={() => {
                        setIsMenuOpen(false);
                      }}
                    />
                  </OutsideClickHandler>
                </EscKeydownHandler>
              </FocusOutHandler>
            )
          }
          placement={PopupPlacement.BottomLeft}
          zLevel={PopupZLevel.Global}
        >
          <ButtonSecondary
            className="sw-max-w-abs-800 sw-px-3"
            onClick={() => {
              setIsMenuOpen(!isMenuOpen);
            }}
            disabled={!isMenuEnabled}
            aria-expanded={isMenuOpen}
            aria-haspopup="menu"
          >
            {currentBranchLikeElement}
          </ButtonSecondary>
        </Popup>

        <div className="sw-ml-2">
          <BranchHelpTooltip
            component={component}
            isApplication={isApplication}
            hasManyBranches={hasManyBranches}
            canAdminComponent={canAdminComponent}
            branchSupportEnabled={branchSupportEnabled}
          />
        </div>

        <PRLink currentBranchLike={currentBranchLike} component={component} />
      </div>
    </>
  );
}

export default withAvailableFeatures(React.memo(BranchLikeNavigation));

const SlashSeparator = styled.span`
  &:after {
    content: '/';
    color: rgba(68, 68, 68, 0.3);
  }
`;
