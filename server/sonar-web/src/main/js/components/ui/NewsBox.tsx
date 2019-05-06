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
import * as classNames from 'classnames';
import { ClearButton } from './buttons';
import { translate } from '../../helpers/l10n';
import './NewsBox.css';

export interface Props {
  children: React.ReactNode;
  className?: string;
  onClose: () => void;
  title: string;
}

export default function NewsBox({ children, className, onClose, title }: Props) {
  return (
    <div className={classNames('news-box', className)} role="alert">
      <div className="news-box-header">
        <div className="display-flex-center">
          <span className="badge badge-new spacer-right">{translate('new')}</span>
          <strong>{title}</strong>
        </div>
        <ClearButton
          className="button-tiny"
          iconProps={{ size: 12, then: true }}
          onClick={onClose}
        />
      </div>
      <div className="big-spacer-top note">{children}</div>
    </div>
  );
}
