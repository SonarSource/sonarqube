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
import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { addGlobalSuccessMessage } from 'design-system/lib';
import {
  getEmailConfigurations,
  getSystemUpgrades,
  patchEmailConfiguration,
  postEmailConfiguration,
} from '../api/system';
import { translate } from '../helpers/l10n';
import { EmailConfiguration } from '../types/system';
import { createQueryHook } from './common';

export const useSystemUpgrades = createQueryHook(() => {
  return queryOptions({
    queryKey: ['system', 'upgrades'],
    queryFn: () => getSystemUpgrades(),
    staleTime: Infinity,
  });
});

export function useGetEmailConfiguration() {
  return useQuery({
    queryKey: ['email_configuration'] as const,
    queryFn: async () => {
      const { emailConfigurations } = await getEmailConfigurations();
      return emailConfigurations && emailConfigurations.length > 0 ? emailConfigurations[0] : null;
    },
  });
}

export function useSaveEmailConfigurationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: EmailConfiguration) => {
      return postEmailConfiguration(data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['email_configuration'] });
      addGlobalSuccessMessage(
        translate('email_notification.form.save_configuration.create_success'),
      );
    },
  });
}

export function useUpdateEmailConfigurationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      emailConfiguration,
      id,
    }: {
      emailConfiguration: EmailConfiguration;
      id: string;
    }) => {
      return patchEmailConfiguration(id, emailConfiguration);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['email_configuration'] });
      addGlobalSuccessMessage(
        translate('email_notification.form.save_configuration.update_success'),
      );
    },
  });
}
