/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { connect } from 'react-redux';
import Checkbox from 'sonar-ui-common/components/controls/Checkbox';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getCurrentUserSetting, Store } from '../../../store/rootReducer';
import { setCurrentUserSetting } from '../../../store/users';

interface Props {
  notificationsOptOut?: boolean;
  setCurrentUserSetting: (setting: T.CurrentUserSetting) => void;
}

export class SonarCloudNotifications extends React.PureComponent<Props> {
  handleCheckOptOut = (checked: boolean) => {
    this.props.setCurrentUserSetting({
      key: 'notifications.optOut',
      value: checked ? 'false' : 'true'
    });
  };

  render() {
    return (
      <section className="boxed-group">
        <h2>{translate('my_profile.sonarcloud_feature_notifications.title')}</h2>
        <div className="boxed-group-inner">
          <table className="data zebra">
            <thead>
              <tr>
                <th />
                <th className="text-center">
                  <h4>{translate('activate')}</h4>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>{translate('my_profile.sonarcloud_feature_notifications.description')}</td>
                <td className="text-center">
                  <Checkbox
                    checked={!this.props.notificationsOptOut}
                    onCheck={this.handleCheckOptOut}
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    );
  }
}

const mapStateToProps = (state: Store) => {
  const notificationsOptOut = getCurrentUserSetting(state, 'notifications.optOut') === 'true';

  return {
    notificationsOptOut
  };
};

const mapDispatchToProps = {
  setCurrentUserSetting
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(SonarCloudNotifications);
