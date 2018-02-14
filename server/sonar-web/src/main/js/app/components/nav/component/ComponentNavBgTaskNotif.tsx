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
import * as React from 'react';
import { Link } from 'react-router';
import { FormattedMessage } from 'react-intl';
import * as PropTypes from 'prop-types';
import NavBarNotif from '../../../../components/nav/NavBarNotif';
import PendingIcon from '../../../../components/icons-components/PendingIcon';
import { Component } from '../../../types';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import { getComponentBackgroundTaskUrl } from '../../../../helpers/urls';
import { hasMessage, translate } from '../../../../helpers/l10n';
import { Task } from '../../../../api/ce';

interface Props {
  component: Component;
  currentTask?: Task;
  isInProgress?: boolean;
  isPending?: boolean;
}

export default class ComponentNavBgTaskNotif extends React.PureComponent<Props> {
  static contextTypes = {
    canAdmin: PropTypes.bool.isRequired
  };

  renderMessage(messageKey: string, status?: string) {
    const { component } = this.props;
    const canSeeBackgroundTasks =
      component.configuration && component.configuration.showBackgroundTasks;
    const bgTaskUrl = getComponentBackgroundTaskUrl(component.key, status);

    if (canSeeBackgroundTasks) {
      return (
        <FormattedMessage
          defaultMessage={translate(messageKey, 'admin')}
          id={messageKey + '.admin'}
          values={{
            url: <Link to={bgTaskUrl}>{translate('background_tasks.page')}</Link>
          }}
        />
      );
    }

    return <span>{translate(messageKey)}</span>;
  }

  render() {
    const { currentTask, isInProgress, isPending } = this.props;

    if (isInProgress) {
      return (
        <NavBarNotif className="alert alert-info">
          <i className="spinner spacer-right text-bottom" />
          {this.renderMessage('component_navigation.status.in_progress')}
        </NavBarNotif>
      );
    } else if (isPending) {
      return (
        <NavBarNotif className="alert alert-info">
          <PendingIcon className="spacer-right" />
          {this.renderMessage('component_navigation.status.pending', STATUSES.ALL)}
        </NavBarNotif>
      );
    } else if (currentTask && currentTask.status === STATUSES.FAILED) {
      if (
        currentTask.errorType &&
        currentTask.errorType.includes('LICENSING') &&
        hasMessage('license.component_navigation.button', currentTask.errorType)
      ) {
        return (
          <NavBarNotif className="alert alert-danger">
            <span className="little-spacer-right">{currentTask.errorMessage}</span>
            {this.context.canAdmin ? (
              <Link to="/admin/extension/license/app">
                {translate('license.component_navigation.button', currentTask.errorType)}.
              </Link>
            ) : (
              translate('please_contact_administrator')
            )}
          </NavBarNotif>
        );
      }

      return (
        <NavBarNotif className="alert alert-danger">
          {this.renderMessage('component_navigation.status.failed')}
        </NavBarNotif>
      );
    }
    return null;
  }
}
