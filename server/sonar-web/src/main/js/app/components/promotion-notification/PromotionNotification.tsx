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
import { ButtonPrimary, ButtonSecondary, themeBorder, themeColor } from 'design-system';
import * as React from 'react';
import { dismissNotice } from '../../../api/users';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { NoticeType, isLoggedIn } from '../../../types/users';
import { CurrentUserContextInterface } from '../current-user/CurrentUserContext';
import withCurrentUserContext from '../current-user/withCurrentUserContext';

export function PromotionNotification(props: CurrentUserContextInterface) {
  const { currentUser, updateDismissedNotices } = props;

  const onClick = React.useCallback(() => {
    return dismissNotice(NoticeType.SONARLINT_AD)
      .then(() => {
        updateDismissedNotices(NoticeType.SONARLINT_AD, true);
      })
      .catch(() => {
        /* noop */
      });
  }, [updateDismissedNotices]);

  if (!isLoggedIn(currentUser) || currentUser.dismissedNotices[NoticeType.SONARLINT_AD]) {
    return null;
  }

  return (
    <PromotionNotificationWrapper className="it__promotion_notification sw-z-global-popup sw-rounded-1 sw-flex sw-items-center sw-px-4">
      <div className="sw-mr-2">
        <img alt="SonarQube + SonarLint" height={80} src={`${getBaseUrl()}/images/sq-sl.svg`} />
      </div>
      <PromotionNotificationContent className="sw-flex-1 sw-px-2 sw-py-4">
        <span className="sw-body-sm-highlight">{translate('promotion.sonarlint.title')}</span>
        <p className="sw-mt-2">{translate('promotion.sonarlint.content')}</p>
      </PromotionNotificationContent>
      <div className="sw-ml-2 sw-pl-2 sw-flex sw-flex-col sw-items-stretch">
        <ButtonPrimary
          className="sw-mb-4"
          to="https://www.sonarsource.com/products/sonarlint/?referrer=sonarqube-welcome"
          onClick={onClick}
        >
          {translate('learn_more')}
        </ButtonPrimary>
        <ButtonSecondary className="sw-justify-center" onClick={onClick}>
          {translate('dismiss')}
        </ButtonSecondary>
      </div>
    </PromotionNotificationWrapper>
  );
}

export default withCurrentUserContext(PromotionNotification);

const PromotionNotificationWrapper = styled.div`
  position: fixed;
  right: 10px;
  bottom: 10px;
  max-width: 600px;
  box-shadow: 1px 1px 5px 0px black;

  background: ${themeColor('promotionNotificationBackground')};
  color: ${themeColor('promotionNotification')};
`;

const PromotionNotificationContent = styled.div`
  border-right: ${themeBorder('default', 'promotionNotificationSeparator')};
`;
