/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import { getIssueChangelog } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
import Avatar from '../../../components/ui/Avatar';
import BubblePopup from '../../../components/common/BubblePopup';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import IssueChangelogDiff from '../components/IssueChangelogDiff';
/*:: import type { ChangelogDiff } from '../components/IssueChangelogDiff'; */
/*:: import type { Issue } from '../types'; */

/*::
type Changelog = {
  avatar?: string,
  creationDate: string,
  diffs: Array<ChangelogDiff>,
  user: string,
  userName: string
};
*/

/*::
type Props = {
  issue: Issue,
  onFail: Error => void,
  popupPosition?: {}
};
*/

/*::
type State = {
  changelogs: Array<Changelog>
};
*/

export default class ChangelogPopup extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    changelogs: []
  };

  componentDidMount() {
    this.mounted = true;
    this.loadChangelog();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadChangelog() {
    getIssueChangelog(this.props.issue.key).then(changelogs => {
      if (this.mounted) {
        this.setState({ changelogs });
      }
    }, this.props.onFail);
  }

  render() {
    const { issue } = this.props;
    const { author } = issue;
    return (
      <BubblePopup position={this.props.popupPosition} customClass="bubble-popup-bottom-right">
        <div className="issue-changelog">
          <table className="spaced">
            <tbody>
              <tr>
                <td className="thin text-left text-top nowrap">
                  <DateTimeFormatter date={issue.creationDate} />
                </td>
                <td className="text-left text-top">
                  {author ? `${translate('created_by')} ${author}` : translate('created')}
                </td>
              </tr>

              {this.state.changelogs.map((item, idx) => (
                <tr key={idx}>
                  <td className="thin text-left text-top nowrap">
                    <DateTimeFormatter date={item.creationDate} />
                  </td>
                  <td className="text-left text-top">
                    {item.userName && (
                      <p>
                        <Avatar
                          className="little-spacer-right"
                          hash={item.avatar}
                          name={item.userName}
                          size={16}
                        />
                        {item.userName}
                      </p>
                    )}
                    {item.diffs.map(diff => <IssueChangelogDiff key={diff.key} diff={diff} />)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </BubblePopup>
    );
  }
}
