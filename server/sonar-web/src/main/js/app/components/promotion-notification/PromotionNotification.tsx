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
import * as React from 'react';
import { dismissNotice } from '../../../api/users';
import { ButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { isLoggedIn, NoticeType } from '../../../types/users';
import { CurrentUserContextInterface } from '../current-user/CurrentUserContext';
import withCurrentUserContext from '../current-user/withCurrentUserContext';
import './PromotionNotification.css';

export function PromotionNotification(props: CurrentUserContextInterface) {
  const { currentUser } = props;

  if (!isLoggedIn(currentUser) || currentUser.dismissedNotices[NoticeType.SONARLINT_AD]) {
    return null;
  }

  const onClick = () => {
    dismissNotice(NoticeType.SONARLINT_AD)
      .then(() => {
        props.updateDismissedNotices(NoticeType.SONARLINT_AD, true);
      })
      .catch(() => {
        /* noop */
      });
  };

  return (
    <div className="toaster display-flex-center big-padded-left big-padded-right">
      <div className="toaster-icon spacer-right">
        <img alt="SonarQube + SonarLint" height={80} src={`${getBaseUrl()}/images/sq-sl.svg`} />
      </div>
      <div className="toaster-content flex-1 padded-left padded-right big-padded-top big-padded-bottom">
        <span className="toaster-title text-bold medium">
          {translate('promotion.sonarlint.title')}
        </span>
        <p className="spacer-top">{translate('promotion.sonarlint.content')}</p>
      </div>
      <div className="toaster-actions spacer-left padded-left display-flex-column display-flex-center">
        <a
          className="button button-primary big-spacer-bottom"
          href="https://www.sonarsource.com/products/sonarlint/?referrer=sonarqube-welcome"
          rel="noreferrer"
          onClick={onClick}
          target="_blank"
        >
          {translate('learn_more')}
        </a>
        <ButtonLink className="toaster-link" onClick={onClick}>
          {translate('dismiss')}
        </ButtonLink>
      </div>
    </div>
  );
}

export default withCurrentUserContext(PromotionNotification);
