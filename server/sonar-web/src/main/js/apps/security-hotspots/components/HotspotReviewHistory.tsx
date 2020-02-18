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
import { sanitize } from 'dompurify';
import * as React from 'react';
import { Button, DeleteButton } from 'sonar-ui-common/components/controls/buttons';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import IssueChangelogDiff from '../../../components/issue/components/IssueChangelogDiff';
import Avatar from '../../../components/ui/Avatar';
import { Hotspot, ReviewHistoryType } from '../../../types/security-hotspots';
import { getHotspotReviewHistory } from '../utils';

export interface HotspotReviewHistoryProps {
  hotspot: Hotspot;
  onDeleteComment: (key: string) => void;
}

export default function HotspotReviewHistory(props: HotspotReviewHistoryProps) {
  const { hotspot } = props;
  const reviewHistory = getHotspotReviewHistory(hotspot);

  return (
    <>
      {reviewHistory.map((historyElt, historyIndex) => {
        const { user, type, diffs, date, html, key, updatable } = historyElt;
        return (
          <div
            className={classNames('padded', { 'bordered-top': historyIndex > 0 })}
            key={historyIndex}>
            <div className="display-flex-center">
              {user.name && (
                <>
                  <Avatar
                    className="little-spacer-right"
                    hash={user.avatar}
                    name={user.name}
                    size={20}
                  />
                  <strong>
                    {user.active ? user.name : translateWithParameters('user.x_deleted', user.name)}
                  </strong>
                  {type === ReviewHistoryType.Creation && (
                    <span className="little-spacer-left">
                      {translate('hotspots.review_history.created')}
                    </span>
                  )}
                  {type === ReviewHistoryType.Comment && (
                    <span className="little-spacer-left">
                      {translate('hotspots.review_history.comment_added')}
                    </span>
                  )}
                  <span className="little-spacer-left little-spacer-right">-</span>
                </>
              )}
              <DateTimeFormatter date={date} />
            </div>

            {type === ReviewHistoryType.Diff && diffs && (
              <div className="spacer-top">
                {diffs.map((diff, diffIndex) => (
                  <IssueChangelogDiff diff={diff} key={diffIndex} />
                ))}
              </div>
            )}

            {type === ReviewHistoryType.Comment && key && html && (
              <div className="spacer-top display-flex-space-between">
                <div className="markdown" dangerouslySetInnerHTML={{ __html: sanitize(html) }} />
                {updatable && (
                  <div className="dropdown">
                    <Dropdown
                      overlay={
                        <div className="padded abs-width-150">
                          <p>{translate('issue.comment.delete_confirm_message')}</p>
                          <Button
                            className="button-red big-spacer-top pull-right"
                            onClick={() => props.onDeleteComment(key)}>
                            {translate('delete')}
                          </Button>
                        </div>
                      }
                      overlayPlacement={PopupPlacement.BottomRight}>
                      <DeleteButton className="it__hotspots-comment-delete button-small" />
                    </Dropdown>
                  </div>
                )}
              </div>
            )}
          </div>
        );
      })}
    </>
  );
}
