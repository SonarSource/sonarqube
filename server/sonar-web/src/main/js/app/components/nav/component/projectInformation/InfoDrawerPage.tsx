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
import * as classNames from 'classnames';
import * as React from 'react';
import BackIcon from 'sonar-ui-common/components/icons/BackIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface InfoDrawerPageProps {
  children: React.ReactNode;
  displayed: boolean;
  onPageChange: () => void;
}

export default function InfoDrawerPage(props: InfoDrawerPageProps) {
  const { children, displayed, onPageChange } = props;
  return (
    <div
      className={classNames(
        'info-drawer-page info-drawer-pane display-flex-column overflow-hidden',
        {
          open: displayed
        }
      )}>
      <h2 className="back-button big-padded bordered-bottom" onClick={() => onPageChange()}>
        <BackIcon className="little-spacer-right" />
        {translate('back')}
      </h2>

      {displayed && <div className="overflow-y-auto big-padded">{children}</div>}
    </div>
  );
}
