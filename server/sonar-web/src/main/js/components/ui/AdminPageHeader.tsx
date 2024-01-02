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
import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import { themeColor } from 'design-system';
import React, { ReactNode } from 'react';

interface Props {
  children?: ReactNode;
  className?: string;
  description?: ReactNode;
  title: ReactNode;
}

export function AdminPageHeader({ children, className, description, title }: Readonly<Props>) {
  return (
    <div className={classNames('sw-flex sw-justify-between', className)}>
      <header className="sw-flex-1">
        <AdminPageTitle className="sw-heading-lg">{title}</AdminPageTitle>
        {description && (
          <AdminPageDescription className="sw-body-sm sw-pt-4 sw-max-w-9/12">
            {description}
          </AdminPageDescription>
        )}
      </header>
      {children && <div className="sw-flex sw-gap-2">{children}</div>}
    </div>
  );
}
export const AdminPageTitle = withTheme(styled.h1`
  color: ${themeColor('pageTitle')};
`);

export const AdminPageDescription = withTheme(styled.div`
  color: ${themeColor('pageContent')};
`);
