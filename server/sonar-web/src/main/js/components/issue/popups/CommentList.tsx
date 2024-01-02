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
import { translate } from '../../../helpers/l10n';
import { IssueComment } from '../../../types/types';
import CommentTile from './CommentTile';

interface CommentListProps {
  comments?: IssueComment[];
  deleteComment: (comment: string) => void;
  onEdit: (comment: string, text: string) => void;
}

export default function CommentList(props: CommentListProps) {
  const { comments } = props;
  if (!comments || comments.length === 0) {
    return (
      <div className="note spacer-top spacer-bottom">{translate('issue.comment.empty.list')}</div>
    );
  }

  // sorting comment i.e showing newest on top
  const sortedComments = [...comments]?.sort(
    (com1, com2) =>
      new Date(com2.createdAt || '').getTime() - new Date(com1.createdAt || '').getTime()
  );
  return (
    <div className="issue-comment-list-wrapper spacer-bottom">
      {sortedComments?.map((c) => (
        <CommentTile
          comment={c}
          key={c.key}
          handleDelete={props.deleteComment}
          onEdit={props.onEdit}
        />
      ))}
    </div>
  );
}
