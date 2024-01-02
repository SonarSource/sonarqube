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
import { ButtonIcon } from '../../components/controls/buttons';
import ClearIcon from '../../components/icons/ClearIcon';
import { Alert, AlertProps } from '../../components/ui/Alert';
import { translate } from '../../helpers/l10n';
import { get, save } from '../../helpers/storage';
import './DismissableAlert.css';

export interface DismissableAlertProps extends AlertProps {
  alertKey: string;
  children?: React.ReactNode;
  className?: string;
}

export const DISMISSED_ALERT_STORAGE_KEY = 'sonarqube.dismissed_alert';

export default function DismissableAlert(props: DismissableAlertProps) {
  const { alertKey, children, className, display = 'banner', variant } = props;
  const [show, setShow] = React.useState(false);

  React.useEffect(() => {
    if (get(DISMISSED_ALERT_STORAGE_KEY, alertKey) !== 'true') {
      setShow(true);
    }
  }, [alertKey]);

  const hideAlert = () => {
    window.dispatchEvent(new Event('resize'));
    save(DISMISSED_ALERT_STORAGE_KEY, 'true', alertKey);
  };

  return !show ? null : (
    <div className={classNames('dismissable-alert-wrapper', className)}>
      <Alert className={`dismissable-alert-${display}`} display={display} variant={variant}>
        <div className="display-flex-center dismissable-alert-content">
          <div className="flex-1">{children}</div>
          <ButtonIcon
            aria-label={translate('alert.dismiss')}
            onClick={() => {
              hideAlert();
              setShow(false);
            }}
          >
            <ClearIcon size={12} thin={true} />
          </ButtonIcon>
        </div>
      </Alert>
    </div>
  );
}
