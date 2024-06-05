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
import { Tooltip } from '@sonarsource/echoes-react';
import {
  ButtonPrimary,
  ButtonSecondary,
  DangerButtonSecondary,
  FlagMessage,
  SubHeading,
} from 'design-system';
import React, { ReactElement } from 'react';
import { translate } from '../../../../helpers/l10n';

interface Props {
  canDisable: boolean;
  enabled: boolean;
  extraActions?: ReactElement;
  isDeleting: boolean;
  onDelete: () => void;
  onEdit: () => void;
  onToggle: () => void;
  title: string;
  url: string | string[] | undefined;
}

export default function ConfigurationDetails(props: Readonly<Props>) {
  const { title, url, canDisable, onEdit, onDelete, onToggle, extraActions, isDeleting, enabled } =
    props;

  return (
    <div className="sw-flex sw-mb-6 sw-justify-between">
      <div className="sw-min-w-0">
        <SubHeading as="h5" title={title} className="sw-truncate">
          {title}
        </SubHeading>
        <p>{url}</p>
        {enabled ? (
          <ButtonSecondary className="sw-mt-4" onClick={onToggle} disabled={!canDisable}>
            {translate('settings.authentication.form.disable')}
          </ButtonSecondary>
        ) : (
          <ButtonPrimary className="sw-mt-4" onClick={onToggle} disabled={!canDisable}>
            {translate('settings.authentication.form.enable')}
          </ButtonPrimary>
        )}
        {!canDisable && (
          <FlagMessage className="sw-mt-2" variant="warning">
            {translate('settings.authentication.form.disable.tooltip')}
          </FlagMessage>
        )}
      </div>
      <div className="sw-flex sw-gap-2 sw-flex-nowrap sw-shrink-0">
        {extraActions}
        <ButtonSecondary onClick={onEdit}>
          {translate('settings.authentication.form.edit')}
        </ButtonSecondary>
        <Tooltip
          content={
            enabled || isDeleting ? translate('settings.authentication.form.delete.tooltip') : null
          }
        >
          <DangerButtonSecondary disabled={enabled || isDeleting} onClick={onDelete}>
            {translate('settings.authentication.form.delete')}
          </DangerButtonSecondary>
        </Tooltip>
      </div>
    </div>
  );
}
