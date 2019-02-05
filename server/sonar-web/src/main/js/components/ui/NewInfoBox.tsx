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
import { ButtonIcon } from './buttons';
import ClearIcon from '../icons-components/ClearIcon';
import { sonarcloudBlack500 } from '../../app/theme';
import { translate } from '../../helpers/l10n';
import './NewInfoBox.css';

export interface Props {
  children: React.ReactNode;
  className?: string;
  description: React.ReactNode;
  onClose?: () => void;
  title: string;
}

export default function NewInfoBox({ className, children, description, onClose, title }: Props) {
  return (
    <div className={classNames('new-info-box', className)} role="alert">
      <div className="new-info-box-inner text-left">
        <div className="new-info-box-header spacer-bottom">
          <span className="display-inline-flex-center">
            <span className="badge badge-new spacer-right">{translate('new')}</span>
            <strong>{title}</strong>
          </span>
        </div>
        <p className="note spacer-bottom">{description}</p>
        {children}
      </div>
      <ButtonIcon className="button-small spacer-left" color={sonarcloudBlack500} onClick={onClose}>
        <ClearIcon size={12} />
      </ButtonIcon>
    </div>
  );
}
