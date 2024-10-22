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
import React from 'react';
import { useIntl } from 'react-intl';
import {
  Card,
  CheckIcon,
  CloseIcon,
  DiscreetInteractiveIcon,
  LightLabel,
  ListItem,
  StandoutLink,
  SubTitle,
  SubnavigationFlowSeparator,
} from '~design-system';
import { Status } from '~sonar-aligned/types/common';
import { useCurrentUser } from '../../../app/components/current-user/CurrentUserContext';
import useLocalStorage from '../../../hooks/useLocalStorage';
import { isLoggedIn } from '../../../types/users';
import { Status as QGStatus } from '../utils';

interface Props {
  status?: Status;
}

const SONARLINT_PR_LS_KEY = 'sonarqube.pr_overview.show_sonarlint_promotion';

export default function SonarLintAd({ status }: Readonly<Props>) {
  const intl = useIntl();
  const { currentUser } = useCurrentUser();
  const [showSLPromotion, setSLPromotion] = useLocalStorage(SONARLINT_PR_LS_KEY, true);

  const onDismiss = React.useCallback(() => {
    setSLPromotion(false);
  }, [setSLPromotion]);

  if (
    !isLoggedIn(currentUser) ||
    currentUser.usingSonarLintConnectedMode ||
    status !== QGStatus.ERROR ||
    !showSLPromotion
  ) {
    return null;
  }

  return (
    <StyledSummaryCard className="it__overview__sonarlint-promotion sw-flex sw-flex-col sw-mt-4">
      <div className="sw-flex sw-justify-between">
        <SubTitle as="h2" className="sw-typo-lg-semibold">
          {intl.formatMessage({ id: 'overview.sonarlint_ad.header' })}
        </SubTitle>
        <DiscreetInteractiveIcon
          Icon={CloseIcon}
          aria-label={intl.formatMessage({ id: 'overview.sonarlint_ad.close_promotion' })}
          onClick={onDismiss}
          size="medium"
        />
      </div>
      <ul>
        <TickLink message={intl.formatMessage({ id: 'overview.sonarlint_ad.details_1' })} />
        <TickLink message={intl.formatMessage({ id: 'overview.sonarlint_ad.details_2' })} />
        <TickLink message={intl.formatMessage({ id: 'overview.sonarlint_ad.details_3' })} />
        <TickLink message={intl.formatMessage({ id: 'overview.sonarlint_ad.details_4' })} />
        <TickLink
          className="sw-typo-semibold"
          message={intl.formatMessage({ id: 'overview.sonarlint_ad.details_5' })}
        />
      </ul>
      <SubnavigationFlowSeparator className="sw-mb-4" />
      <div>
        <StandoutLink
          className="sw-text-left sw-typo-semibold"
          to="https://www.sonarsource.com/products/sonarlint/features/connected-mode/?referrer=sonarqube"
        >
          {intl.formatMessage({ id: 'overview.sonarlint_ad.learn_more' })}
        </StandoutLink>
      </div>
    </StyledSummaryCard>
  );
}

function TickLink({ className, message }: Readonly<{ className?: string; message: string }>) {
  return (
    <ListItem className={`sw-typo-default ${className}`}>
      <CheckIcon fill="iconTrendPositive" />
      <LightLabel className="sw-pl-1">{message}</LightLabel>
    </ListItem>
  );
}

const StyledSummaryCard = styled(Card)`
  background-color: transparent;
`;
