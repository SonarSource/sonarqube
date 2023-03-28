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
import * as React from 'react';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import { ButtonLink } from '../../../components/controls/buttons';
import Toggler from '../../../components/controls/Toggler';
import DropdownIcon from '../../../components/icons/DropdownIcon';
import { translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import { formatterOption } from '../../intl/DateFormatter';
import DateFromNow from '../../intl/DateFromNow';
import ChangelogPopup from '../popups/ChangelogPopup';

export interface IssueChangelogProps extends WrappedComponentProps {
  isOpen: boolean;
  issue: Pick<Issue, 'author' | 'creationDate' | 'key'>;
  creationDate: string;
  togglePopup: (popup: string, show?: boolean) => void;
}

function IssueChangelog(props: IssueChangelogProps) {
  const {
    isOpen,
    issue,
    creationDate,
    intl: { formatDate },
  } = props;
  return (
    <div className="dropdown">
      <Toggler
        onRequestClose={() => {
          props.togglePopup('changelog', false);
        }}
        open={isOpen}
        overlay={<ChangelogPopup issue={issue} />}
      >
        <ButtonLink
          aria-expanded={isOpen}
          aria-label={translateWithParameters(
            'issue.changelog.found_on_x_show_more',
            formatDate(creationDate, formatterOption)
          )}
          className="issue-action issue-action-with-options js-issue-show-changelog"
          onClick={() => {
            props.togglePopup('changelog');
          }}
        >
          <span className="issue-meta-label">
            <DateFromNow date={creationDate} />
          </span>
          <DropdownIcon className="little-spacer-left" />
        </ButtonLink>
      </Toggler>
    </div>
  );
}

export default injectIntl(IssueChangelog);
