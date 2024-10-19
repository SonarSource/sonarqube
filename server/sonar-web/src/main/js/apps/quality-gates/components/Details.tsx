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
import { Spinner } from '@sonarsource/echoes-react';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { useQualityGateQuery } from '../../../queries/quality-gates';
import DetailsContent from './DetailsContent';
import DetailsHeader from './DetailsHeader';

interface Props {
  organization: string;
  qualityGateName: string;
}

export default function Details({ organization, qualityGateName }: Readonly<Props>) {
  const { data: qualityGate, isLoading, isFetching } = useQualityGateQuery(organization, qualityGateName);

  return (
    <Spinner wrapperClassName="sw-block sw-text-center" isLoading={isLoading}>
      {qualityGate && (
        <main>
          <Helmet defer={false} title={qualityGate.name} />
          <DetailsHeader organization={organization} qualityGate={qualityGate} />
          <DetailsContent organization={organization} qualityGate={qualityGate} isFetching={isFetching} />
        </main>
      )}
    </Spinner>
  );
}
