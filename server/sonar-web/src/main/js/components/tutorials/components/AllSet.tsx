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
import { animated, config, useSpring } from '@react-spring/web';
import { CheckIcon, FlagVisual, SubTitle } from 'design-system';
import * as React from 'react';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import useIntersectionObserver from '../../../hooks/useIntersectionObserver';
import { AlmKeys } from '../../../types/alm-settings';
import { Feature } from '../../../types/features';

export interface AllSetProps extends WithAvailableFeaturesProps {
  alm: AlmKeys;
  willRefreshAutomatically?: boolean;
}

export function AllSet(props: AllSetProps) {
  const outroRef = React.useRef<HTMLDivElement>(null);
  const { alm, willRefreshAutomatically } = props;
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);

  const intersectionEntry = useIntersectionObserver(outroRef, { freezeOnceVisible: true });

  const outroAnimation = useSpring({
    from: { top: '200px' },
    to: intersectionEntry?.isIntersecting ? { top: '0px' } : { top: '200px' },
    config: config.wobbly,
  });

  return (
    <animated.div
      className="sw-flex sw-flex-col sw-items-center sw-relative"
      ref={outroRef}
      style={outroAnimation}
    >
      <FlagVisual />
      <SubTitle className="sw-mt-3 sw-mb-12">
        {translate('onboarding.tutorial.ci_outro.done')}
      </SubTitle>
      <MessageContainer>
        <p className="sw-body-sm sw-mb-4">
          {translate('onboarding.tutorial.ci_outro.refresh_text')}
        </p>
        <ul className="sw-mb-6">
          <li className="sw-mb-4 sw-flex">
            <CheckIcon className="sw-mr-2 sw-pt-1/2" />
            {branchSupportEnabled
              ? translate('onboarding.tutorial.ci_outro.commit.why', alm)
              : translate('onboarding.tutorial.ci_outro.commit.why.no_branches')}
          </li>
          {willRefreshAutomatically && (
            <li className="sw-mb-4 sw-flex">
              <CheckIcon className="sw-mr-2 sw-pt-1/2" />
              {translate('onboarding.tutorial.ci_outro.refresh.why')}
            </li>
          )}
        </ul>
      </MessageContainer>
    </animated.div>
  );
}

const MessageContainer = styled.div`
  width: 840px;
`;

export default withAvailableFeatures(AllSet);
