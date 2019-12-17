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
import * as React from 'react';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import IssueChangelogDiff from '../../../components/issue/components/IssueChangelogDiff';
import Avatar from '../../../components/ui/Avatar';
import { ReviewHistoryElement, ReviewHistoryType } from '../../../types/security-hotspots';

export interface HotspotViewerReviewHistoryTabProps {
  history: ReviewHistoryElement[];
}

export default function HotspotViewerReviewHistoryTab(props: HotspotViewerReviewHistoryTabProps) {
  const { history } = props;

  return (
    <>
      {history.map((elt, i) => (
        <React.Fragment key={`${elt.user.name}-${elt.date}`}>
          {i > 0 && <hr />}
          <div>
            <div className="display-flex-center">
              {elt.user.name && (
                <>
                  <Avatar
                    className="little-spacer-right"
                    hash={elt.user.avatar}
                    name={elt.user.name}
                    size={20}
                  />
                  <strong>
                    {elt.user.active
                      ? elt.user.name
                      : translateWithParameters('user.x_deleted', elt.user.name)}
                  </strong>
                  {elt.type === ReviewHistoryType.Creation && (
                    <span className="little-spacer-left">
                      {translate('hotspots.tabs.review_history.created')}
                    </span>
                  )}
                  <span className="little-spacer-left little-spacer-right">-</span>
                </>
              )}
              <DateTimeFormatter date={elt.date} />
            </div>

            {elt.type === ReviewHistoryType.Diff && elt.diffs && (
              <div className="spacer-top">
                {elt.diffs.map(diff => (
                  <IssueChangelogDiff
                    diff={diff}
                    key={`${diff.key}-${diff.oldValue}-${diff.newValue}`}
                  />
                ))}
              </div>
            )}
          </div>
        </React.Fragment>
      ))}
    </>
  );
}
