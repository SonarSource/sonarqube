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
import { BasicSeparator, FlagVisual, Link } from 'design-system';
import * as React from 'react';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';
import useIntersectionObserver from '../../../hooks/useIntersectionObserver';
import { Component } from '../../../types/types';

export interface DoneNextStepsProps {
  component: Component;
}

export default function DoneNextSteps({ component }: DoneNextStepsProps) {
  const isProjectAdmin = component.configuration?.showSettings;
  const outroRef = React.useRef<HTMLDivElement>(null);

  const intersectionEntry = useIntersectionObserver(outroRef, { freezeOnceVisible: true });

  const outroAnimation = useSpring({
    from: { top: '200px' },
    to: intersectionEntry?.isIntersecting ? { top: '0px' } : { top: '200px' },
    config: config.wobbly,
  });

  const docUrl = useDocUrl();

  return (
    <animated.div
      className="sw-flex sw-flex-col sw-items-center sw-relative"
      ref={outroRef}
      style={outroAnimation}
    >
      <BasicSeparator className="sw-my-8" />
      <StyledDiv>
        <div className="sw-flex sw-justify-center sw-mb-12">
          <FlagVisual />
        </div>

        <p>
          <strong className="sw-font-semibold sw-mr-1">
            {translate('onboarding.analysis.auto_refresh_after_analysis.done')}
          </strong>
          {translate('onboarding.analysis.auto_refresh_after_analysis.auto_refresh')}
        </p>
        <p className="sw-mt-4">
          {isProjectAdmin
            ? translate(
                'onboarding.analysis.auto_refresh_after_analysis.set_up_pr_deco_and_ci.admin',
              )
            : translate('onboarding.analysis.auto_refresh_after_analysis.set_up_pr_deco_and_ci')}
        </p>
        <div className="sw-mt-4">
          <span>
            {translate('onboarding.analysis.auto_refresh_after_analysis.check_these_links')}
          </span>
          <ul className="sw-flex sw-flex-col sw-gap-2 sw-mt-2">
            <li>
              <Link to={docUrl('/analyzing-source-code/branches/branch-analysis/')}>
                {translate(
                  'onboarding.analysis.auto_refresh_after_analysis.check_these_links.branches',
                )}
              </Link>
            </li>

            <li>
              <Link to={docUrl('/analyzing-source-code/pull-request-analysis')}>
                {translate(
                  'onboarding.analysis.auto_refresh_after_analysis.check_these_links.pr_analysis',
                )}
              </Link>
            </li>
          </ul>
        </div>
      </StyledDiv>
    </animated.div>
  );
}

const StyledDiv = styled.div`
  width: 840px;
  margin: auto;
`;
