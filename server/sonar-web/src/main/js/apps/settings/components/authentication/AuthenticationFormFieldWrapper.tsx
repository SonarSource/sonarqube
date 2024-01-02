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
import React, { PropsWithChildren } from 'react';
import MandatoryFieldMarker from '../../../../components/ui/MandatoryFieldMarker';

interface Props {
  readonly title: string;
  readonly description?: string;
  readonly defKey?: string;
  readonly mandatory?: boolean;
}

export default function AuthenticationFormFieldWrapper(props: PropsWithChildren<Props>) {
  const { mandatory = false, title, description, defKey, children } = props;

  return (
    <div className="settings-definition">
      <div className="settings-definition-left">
        <label className="h3" htmlFor={defKey}>
          {title}
        </label>
        {mandatory && <MandatoryFieldMarker />}
        {description && <div className="markdown small spacer-top">{description}</div>}
      </div>
      <div className="settings-definition-right big-padded-top display-flex-column">{children}</div>
    </div>
  );
}
