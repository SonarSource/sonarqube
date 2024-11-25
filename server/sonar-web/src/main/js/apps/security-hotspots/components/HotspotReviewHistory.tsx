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
import { Button, ButtonLink, DeleteButton, EditButton } from '../../../components/controls/buttons';
import Dropdown, { DropdownOverlay } from '../../../components/controls/Dropdown';
import Toggler from '../../../components/controls/Toggler';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import IssueChangelogDiff from '../../../components/issue/components/IssueChangelogDiff';
import Avatar from '../../../components/ui/Avatar';
import { PopupPlacement } from '../../../components/ui/popups';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { SafeHTMLInjection, SanitizeLevel } from '../../../helpers/sanitize';
import { Hotspot, ReviewHistoryType } from '../../../types/security-hotspots';
import { getHotspotReviewHistory } from '../utils';
import HotspotCommentPopup from './HotspotCommentPopup';

export interface HotspotReviewHistoryProps {
  hotspot: Hotspot;
  onDeleteComment: (key: string) => void;
  onEditComment: (key: string, comment: string) => void;
  onShowFullHistory: () => void;
  showFullHistory: boolean;
}

export const MAX_RECENT_ACTIVITY = 5;

export default function HotspotReviewHistory(props: HotspotReviewHistoryProps) {
  const { hotspot, showFullHistory } = props;
  const fullReviewHistory = getHotspotReviewHistory(hotspot);
  const [editedCommentKey, setEditedCommentKey] = React.useState('');

  const reviewHistory = showFullHistory
    ? fullReviewHistory
    : fullReviewHistory.slice(0, MAX_RECENT_ACTIVITY);

  return (
    <>
      <ul>
        {reviewHistory.map((historyElt, historyIndex) => {
          const { user, type, diffs, date, html, key, updatable, markdown } = historyElt;
          return (
            <li
              className={classNames('padded-top padded-bottom', {
                'bordered-top': historyIndex > 0,
              })}
              key={historyIndex}
            >
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
                      {user.active
                        ? user.name
                        : translateWithParameters('user.x_deleted', user.name)}
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

              {type === ReviewHistoryType.Comment && key && html && markdown && (
                <div className="spacer-top display-flex-space-between">
                  <SafeHTMLInjection htmlAsString={html} sanitizeLevel={SanitizeLevel.USER_INPUT}>
                    <div className="markdown" />
                  </SafeHTMLInjection>

                  {updatable && (
                    <div>
                      <div className="dropdown">
                        <Toggler
                          onRequestClose={() => {
                            setEditedCommentKey('');
                          }}
                          open={key === editedCommentKey}
                          overlay={
                            <DropdownOverlay placement={PopupPlacement.BottomRight}>
                              <HotspotCommentPopup
                                markdownComment={markdown}
                                onCancelEdit={() => setEditedCommentKey('')}
                                onCommentEditSubmit={(comment) => {
                                  setEditedCommentKey('');
                                  props.onEditComment(key, comment);
                                }}
                              />
                            </DropdownOverlay>
                          }
                        >
                          <EditButton
                            className="button-small"
                            onClick={() => setEditedCommentKey(key)}
                          />
                        </Toggler>
                      </div>
                      <Dropdown
                        onOpen={() => setEditedCommentKey('')}
                        overlay={
                          <div className="padded abs-width-150">
                            <p>{translate('issue.comment.delete_confirm_message')}</p>
                            <Button
                              className="button-red big-spacer-top pull-right"
                              onClick={() => props.onDeleteComment(key)}
                            >
                              {translate('delete')}
                            </Button>
                          </div>
                        }
                        overlayPlacement={PopupPlacement.BottomRight}
                      >
                        <DeleteButton className="button-small" />
                      </Dropdown>
                    </div>
                  )}
                </div>
              )}
            </li>
          );
        })}
      </ul>
      {!showFullHistory && fullReviewHistory.length > MAX_RECENT_ACTIVITY && (
        <ButtonLink className="spacer-top" onClick={props.onShowFullHistory}>
          {translate('show_all')}
        </ButtonLink>
      )}
    </>
  );
}
