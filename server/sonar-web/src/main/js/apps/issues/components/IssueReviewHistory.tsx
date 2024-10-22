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
import * as React from 'react';
import {
  DangerButtonPrimary,
  DestructiveIcon,
  HtmlFormatter,
  InteractiveIcon,
  LightLabel,
  Modal,
  PencilIcon,
  SafeHTMLInjection,
  SanitizeLevel,
  TrashIcon,
  themeBorder,
} from '~design-system';
import { getIssueChangelog } from '../../../api/issues';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import IssueChangelogDiff from '../../../components/issue/components/IssueChangelogDiff';
import Avatar from '../../../components/ui/Avatar';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { ReviewHistoryType } from '../../../types/security-hotspots';
import { Issue, IssueChangelog } from '../../../types/types';
import HotspotCommentModal from '../../security-hotspots/components/HotspotCommentModal';
import { useGetIssueReviewHistory } from '../crossComponentSourceViewer/utils';

export interface HotspotReviewHistoryProps {
  issue: Issue;
  onDeleteComment: (key: string) => void;
  onEditComment: (key: string, comment: string) => void;
}

const getUpdatedChangelog = ({ changelog }: { changelog: IssueChangelog[] }) =>
  changelog.map((changelogItem) => {
    const diffHasIssueStatusChange = changelogItem.diffs.some((diff) => diff.key === 'issueStatus');

    return {
      ...changelogItem,
      // If the diff is an issue status change, we remove deprecated status and resolution diffs
      diffs: changelogItem.diffs.filter(
        (diff) => !(diffHasIssueStatusChange && ['resolution', 'status'].includes(diff.key)),
      ),
    };
  });

export default function IssueReviewHistory(props: Readonly<HotspotReviewHistoryProps>) {
  const { issue } = props;
  const [changeLog, setChangeLog] = React.useState<IssueChangelog[]>([]);
  const history = useGetIssueReviewHistory(issue, changeLog);
  const [editCommentKey, setEditCommentKey] = React.useState('');
  const [deleteCommentKey, setDeleteCommentKey] = React.useState('');

  React.useEffect(() => {
    getIssueChangelog(issue.key).then(
      ({ changelog }) => {
        const updatedChangelog = getUpdatedChangelog({ changelog });

        setChangeLog(updatedChangelog);
      },
      () => {},
    );
  }, [issue]);

  return (
    <ul>
      {history.map((historyElt, historyIndex) => {
        const { user, type, diffs, date, html, key, updatable, markdown } = historyElt;
        return (
          <li className="sw-p-2 sw-typo-default" key={historyIndex}>
            <div className="sw-typo-semibold sw-mb-1">
              <DateTimeFormatter date={date} />
            </div>

            <LightLabel as="div" className="sw-flex sw-gap-2">
              {user.name && (
                <div className="sw-flex sw-items-center sw-gap-1">
                  <Avatar hash={user.avatar} name={user.name} size="xs" />
                  <span className="sw-typo-semibold">
                    {user.active ? user.name : translateWithParameters('user.x_deleted', user.name)}
                  </span>
                </div>
              )}

              {type === ReviewHistoryType.Creation &&
                translate('issue.activity.review_history.created')}

              {type === ReviewHistoryType.Comment &&
                translate('issue.activity.review_history.comment_added')}
            </LightLabel>

            {type === ReviewHistoryType.Diff && diffs && (
              <div className="sw-mt-2">
                {diffs.map((diff, diffIndex) => (
                  <IssueChangelogDiff diff={diff} key={diffIndex} />
                ))}
              </div>
            )}

            {type === ReviewHistoryType.Comment && key && html && markdown && (
              <div className="sw-mt-2 sw-flex sw-justify-between">
                <SafeHTMLInjection htmlAsString={html} sanitizeLevel={SanitizeLevel.USER_INPUT}>
                  <CommentBox className="sw-pl-2 sw-ml-2 sw-typo-default" />
                </SafeHTMLInjection>

                {updatable && (
                  <div className="sw-flex sw-gap-6">
                    <InteractiveIcon
                      Icon={PencilIcon}
                      aria-label={translate('issue.comment.edit')}
                      onClick={() => setEditCommentKey(key)}
                      size="small"
                      stopPropagation={false}
                    />

                    <DestructiveIcon
                      Icon={TrashIcon}
                      aria-label={translate('issue.comment.delete')}
                      onClick={() => setDeleteCommentKey(key)}
                      size="small"
                      stopPropagation={false}
                    />
                  </div>
                )}

                {editCommentKey === key && (
                  <HotspotCommentModal
                    value={markdown}
                    onCancel={() => setEditCommentKey('')}
                    onSubmit={(comment) => {
                      setEditCommentKey('');
                      props.onEditComment(key, comment);
                    }}
                  />
                )}

                {deleteCommentKey === key && (
                  <Modal
                    headerTitle={translate('issue.comment.delete')}
                    onClose={() => setDeleteCommentKey('')}
                    body={<p>{translate('issue.comment.delete_confirm_message')}</p>}
                    primaryButton={
                      <DangerButtonPrimary
                        onClick={() => {
                          setDeleteCommentKey('');
                          props.onDeleteComment(key);
                        }}
                      >
                        {translate('delete')}
                      </DangerButtonPrimary>
                    }
                    secondaryButtonLabel={translate('cancel')}
                  />
                )}
              </div>
            )}
          </li>
        );
      })}
    </ul>
  );
}

const CommentBox = styled(HtmlFormatter)`
  border-left: ${themeBorder('default', 'activityCommentPipe')};
`;
