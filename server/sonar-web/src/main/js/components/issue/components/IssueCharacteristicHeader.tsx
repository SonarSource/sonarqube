/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { IssueCharacteristic, ISSUE_CHARACTERISTIC_TO_FIT_FOR } from '../../../types/issues';
import DocLink from '../../common/DocLink';
import Tooltip from '../../controls/Tooltip';

export interface IssueCharacteristicHeaderProps {
  characteristic: IssueCharacteristic;
  className?: string;
}

export default function IssueCharacteristicHeader({
  characteristic,
  className,
}: IssueCharacteristicHeaderProps) {
  const nextSelectableNode = React.useRef<HTMLElement | undefined | null>();
  const badgeRef = React.useRef<HTMLElement>(null);
  const linkRef = React.useRef<HTMLAnchorElement | null>(null);

  function handleShowTooltip() {
    document.addEventListener('keydown', handleTabPress);
  }

  function handleHideTooltip() {
    document.removeEventListener('keydown', handleTabPress);
    nextSelectableNode.current = undefined;
  }

  function handleTabPress(event: KeyboardEvent) {
    if (event.code !== KeyboardKeys.Tab) {
      return;
    }

    if (event.shiftKey) {
      if (event.target === linkRef.current) {
        (badgeRef.current as HTMLElement).focus();
      }
      return;
    }

    if (nextSelectableNode.current) {
      event.preventDefault();
      nextSelectableNode.current.focus();
    }

    if (event.target === badgeRef.current) {
      event.preventDefault();
      nextSelectableNode.current = badgeRef.current;
      (linkRef.current as HTMLAnchorElement).focus();
    }
  }

  return (
    <div className={classNames('spacer-bottom', className)}>
      <Tooltip
        mouseLeaveDelay={0.25}
        onShow={handleShowTooltip}
        onHide={handleHideTooltip}
        isInteractive={true}
        overlay={
          <div className="padded-bottom">
            {translate('issue.characteristic.description', characteristic)}
            <hr className="big-spacer-top big-spacer-bottom" />
            <div className="display-flex-center">
              <span className="spacer-right">{translate('learn_more')}:</span>
              <DocLink to="/user-guide/issues/" innerRef={linkRef}>
                {translate('issue.characteristic.doc.link')}
              </DocLink>
            </div>
          </div>
        }
      >
        <span className="badge" ref={badgeRef}>
          {translate('issue.characteristic', characteristic)}
        </span>
      </Tooltip>
      <span className="muted spacer-left issue-category-fit">
        {translate('issue.characteristic.fit', ISSUE_CHARACTERISTIC_TO_FIT_FOR[characteristic])}
      </span>
    </div>
  );
}
