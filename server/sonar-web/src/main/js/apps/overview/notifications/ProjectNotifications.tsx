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
import { Button, ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import ModalButton from 'sonar-ui-common/components/controls/ModalButton';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  withNotifications,
  WithNotificationsProps
} from '../../../components/hoc/withNotifications';
import NotificationsList from '../../account/notifications/NotificationsList';

interface Props {
  className?: string;
  component: T.Component;
}

export function ProjectNotifications(props: WithNotificationsProps & Props) {
  const { channels, className, component, loading, notifications, perProjectTypes } = props;

  const header = translate('my_account.notifications');

  const handleAddNotification = ({ channel, type }: { channel: string; type: string }) => {
    props.addNotification({ project: component.key, channel, type });
  };

  const handleRemoveNotification = ({ channel, type }: { channel: string; type: string }) => {
    props.removeNotification({
      project: component.key,
      channel,
      type
    });
  };

  const getCheckboxId = (type: string, channel: string) => {
    return `project-notification-${component.key}-${type}-${channel}`;
  };

  const projectNotifications = notifications.filter(n => n.project && n.project === component.key);

  return (
    <div className={className}>
      <ModalButton
        modal={({ onClose }) => (
          <Modal contentLabel={header} onRequestClose={onClose}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>
            <div className="modal-body">
              <Alert variant="info">{translate('notification.dispatcher.information')}</Alert>

              <DeferredSpinner loading={loading}>
                <table className="data zebra notifications-table">
                  <thead>
                    <tr>
                      <th aria-label={translate('project')} />
                      {channels.map(channel => (
                        <th className="text-center" key={channel}>
                          <h4>{translate('notification.channel', channel)}</h4>
                        </th>
                      ))}
                    </tr>
                  </thead>

                  <NotificationsList
                    channels={channels}
                    checkboxId={getCheckboxId}
                    notifications={projectNotifications}
                    onAdd={handleAddNotification}
                    onRemove={handleRemoveNotification}
                    project={true}
                    types={perProjectTypes}
                  />
                </table>
              </DeferredSpinner>
            </div>
            <footer className="modal-foot">
              <ResetButtonLink className="js-modal-close" onClick={onClose}>
                {translate('close')}
              </ResetButtonLink>
            </footer>
          </Modal>
        )}>
        {({ onClick }) => (
          <Button onClick={onClick}>
            <span data-test="overview__edit-notifications">
              {translate('my_profile.per_project_notifications.edit')}
            </span>
          </Button>
        )}
      </ModalButton>
    </div>
  );
}

export default withNotifications(ProjectNotifications);
