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

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useContext } from 'react';
import { activateScim, deactivateScim, fetchIsScimEnabled } from '../../api/scim-provisioning';
import { AvailableFeaturesContext } from '../../app/components/available-features/AvailableFeaturesContext';
import { Feature } from '../../types/features';

export function useScimStatusQuery() {
  const hasScim = useContext(AvailableFeaturesContext).includes(Feature.Scim);

  return useQuery({
    queryKey: ['identity_provider', 'scim_status'],
    queryFn: () => {
      if (!hasScim) {
        return false;
      }
      return fetchIsScimEnabled();
    },
  });
}

export function useToggleScimMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (activate: boolean) => (activate ? activateScim() : deactivateScim()),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['identity_provider'] });
    },
  });
}
