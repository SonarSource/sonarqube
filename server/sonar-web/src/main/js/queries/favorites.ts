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

import { InfiniteData, useMutation, useQueryClient } from '@tanstack/react-query';
import { ComponentRaw } from '../api/components';
import { addFavorite, removeFavorite } from '../api/favorites';

export function useToggleFavoriteMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ component, addToFavorites }: { addToFavorites: boolean; component: string }) =>
      addToFavorites ? addFavorite(component) : removeFavorite(component),
    onSuccess: (_, { component, addToFavorites }) => {
      // Update the list cache to reflect the new favorite status
      queryClient.setQueriesData(
        { queryKey: ['project', 'list'] },
        getProjectsFavoritesHandler(component, addToFavorites),
      );
      queryClient.invalidateQueries({
        queryKey: ['project', 'list'],
        refetchType: 'none',
      });

      // Silently update component details cache
      queryClient.setQueryData(
        ['project', 'details', component],
        (oldData: { components: ComponentRaw[] }) => {
          if (!oldData) {
            return oldData;
          }
          return {
            ...oldData,
            components: [{ ...oldData.components[0], isFavorite: addToFavorites }],
          };
        },
      );
    },
  });
}

function getProjectsFavoritesHandler(projectKey: string, addedToFavorites: boolean) {
  return (oldData: InfiniteData<{ components: ComponentRaw[] }>) => {
    if (!oldData) {
      return oldData;
    }

    return {
      ...oldData,
      pages: oldData.pages.map((page) => ({
        ...page,
        components: page.components.map((component) => {
          if (component.key === projectKey) {
            return {
              ...component,
              isFavorite: addedToFavorites,
            };
          }

          return component;
        }),
      })),
    };
  };
}
