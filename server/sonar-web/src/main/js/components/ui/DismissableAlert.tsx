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
import { Banner, Variant } from '~design-system';
import { get, save } from '../../helpers/storage';

export interface DismissableAlertProps {
  alertKey: string;
  children?: React.ReactNode;
  className?: string;
  variant: Variant;
}

export const DISMISSED_ALERT_STORAGE_KEY = 'sonarqube.dismissed_alert';

export function DismissableAlert(props: DismissableAlertProps) {
  const { alertKey, children, className, variant } = props;
  const [show, setShow] = React.useState(false);

  React.useEffect(() => {
    if (get(DISMISSED_ALERT_STORAGE_KEY, alertKey) !== 'true') {
      setShow(true);
    } else {
      setShow(false);
    }
  }, [alertKey]);

  const hideAlert = () => {
    window.dispatchEvent(new Event('resize'));
    save(DISMISSED_ALERT_STORAGE_KEY, 'true', alertKey);
  };

  return !show ? null : (
    <Banner
      onDismiss={() => {
        hideAlert();
        setShow(false);
      }}
      className={classNames('sw-w-full', className)}
      variant={variant}
    >
      {children}
    </Banner>
  );
}
