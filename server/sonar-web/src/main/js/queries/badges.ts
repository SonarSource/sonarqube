/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useContext } from 'react';
import { getProjectBadgesToken, renewProjectBadgesToken } from '../api/project-badges';
import { MetricsContext } from '../app/components/metrics/MetricsContext';
import { getLocalizedMetricName } from '../helpers/l10n';
import { MetricKey } from '../types/metrics';
import { useWebApiQuery } from './web-api';

export function useRenewBagdeTokenMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (key: string) => {
      await renewProjectBadgesToken(key);
    },
    onSuccess: (_, key) => {
      queryClient.invalidateQueries({ queryKey: ['badges-token', key], refetchType: 'all' });
    },
  });
}

export function useBadgeMetricsQuery() {
  const metrics = useContext(MetricsContext);
  const { data: webservices = [], ...rest } = useWebApiQuery();
  const domain = webservices.find((d) => d.path === 'api/project_badges');
  const ws = domain?.actions.find((w) => w.key === 'measure');
  const param = ws?.params?.find((p) => p.key === 'metric');
  if (param?.possibleValues) {
    return {
      ...rest,
      data: param.possibleValues.map((key) => {
        const metric = metrics[key];
        return {
          value: key as MetricKey,
          label: metric ? getLocalizedMetricName(metric) : key,
        };
      }),
    };
  }
  return { ...rest, data: [] };
}

export function useBadgeTokenQuery(componentKey: string) {
  return useQuery(['badges-token', componentKey] as const, ({ queryKey: [_, key] }) =>
    getProjectBadgesToken(key)
  );
}
