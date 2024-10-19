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
import * as React from 'react';
import { Component } from '../../../types/types';
import StatPendingCount from './StatPendingCount';
import StatPendingTime from './StatPendingTime';
import StatStillFailing from './StatStillFailing';

export interface Props {
  component?: Pick<Component, 'key'>;
  failingCount?: number;
  onCancelAllPending: () => void;
  onShowFailing: (e: React.SyntheticEvent<HTMLAnchorElement>) => void;
  pendingCount?: number;
  pendingTime?: number;
}

export default function Stats({ component, pendingCount, pendingTime, ...props }: Readonly<Props>) {
  return (
    <section className="sw-flex sw-items-center sw-my-4 sw-gap-8 sw-typo-lg">
      <StatPendingCount onCancelAllPending={props.onCancelAllPending} pendingCount={pendingCount} />
      {!component && (
        <>
          <StatPendingTime pendingCount={pendingCount} pendingTime={pendingTime} />
          <StatStillFailing failingCount={props.failingCount} onShowFailing={props.onShowFailing} />
        </>
      )}
    </section>
  );
}
