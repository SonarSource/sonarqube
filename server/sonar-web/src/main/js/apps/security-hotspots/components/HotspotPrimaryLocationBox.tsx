/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { ButtonLink } from '../../../components/controls/buttons';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import { translate } from '../../../helpers/l10n';
import { isLoggedIn } from '../../../helpers/users';
import { Hotspot } from '../../../types/security-hotspots';
import { CurrentUser } from '../../../types/types';
import './HotspotPrimaryLocationBox.css';

export interface HotspotPrimaryLocationBoxProps {
  hotspot: Hotspot;
  onCommentClick: () => void;
  currentUser: CurrentUser;
  scroll: (element: HTMLElement, offset?: number) => void;
}

export function HotspotPrimaryLocationBox(props: HotspotPrimaryLocationBoxProps) {
  const { hotspot, currentUser } = props;
  return (
    <div
      className={classNames(
        'hotspot-primary-location',
        'display-flex-space-between display-flex-center padded-top padded-bottom big-padded-left big-padded-right',
        `hotspot-risk-exposure-${hotspot.rule.vulnerabilityProbability}`
      )}
      ref={element => element && props.scroll(element)}>
      <div className="text-bold">{hotspot.message}</div>
      {isLoggedIn(currentUser) && (
        <ButtonLink
          className="nowrap big-spacer-left it__hs-add-comment"
          onClick={props.onCommentClick}>
          {translate('hotspots.comment.open')}
        </ButtonLink>
      )}
    </div>
  );
}

export default withCurrentUser(HotspotPrimaryLocationBox);
