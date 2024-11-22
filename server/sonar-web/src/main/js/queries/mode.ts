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

import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { useIntl } from 'react-intl';
import { addGlobalSuccessMessage } from '~design-system';
import { getMode, updateMode } from '../api/mode';
import { Mode, ModeResponse } from '../types/mode';
import { createQueryHook } from './common';

const useModeQuery = createQueryHook(() => {
  return queryOptions({
    queryKey: ['mode'] as const,
    queryFn: getMode,
    staleTime: Infinity,
  });
});

export const useStandardExperienceModeQuery = () => {
  return useModeQuery({ select: (data) => data.mode === Mode.Standard });
};

export const useModeModifiedQuery = () => {
  return useModeQuery({ select: (data) => data.modified });
};

export function useUpdateModeMutation() {
  const queryClient = useQueryClient();
  const intl = useIntl();
  return useMutation({
    mutationFn: (mode: Mode) => updateMode(mode),
    onSuccess: (res) => {
      queryClient.setQueryData<ModeResponse>(['mode'], res);
      addGlobalSuccessMessage(
        intl.formatMessage(
          {
            id: 'settings.mode.save.success',
          },
          { isStandardMode: res.mode === Mode.Standard },
        ),
      );
    },
  });
}
