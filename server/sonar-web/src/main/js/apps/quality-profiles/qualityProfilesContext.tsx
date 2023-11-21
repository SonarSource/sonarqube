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
import React from 'react';
import { useOutletContext } from 'react-router-dom';
import { Actions } from '../../api/quality-profiles';
import { Exporter, Profile } from '../../apps/quality-profiles/types';
import { getWrappedDisplayName } from '../../components/hoc/utils';
import { Language } from '../../types/languages';

export interface QualityProfilesContextProps {
  actions: Actions;
  exporters: Exporter[];
  languages: Language[];
  profile?: Profile;
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

export function withQualityProfilesContext<P extends Partial<QualityProfilesContextProps>>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<React.PropsWithChildren<P>>>,
): React.ComponentType<
  React.PropsWithChildren<React.PropsWithChildren<Omit<P, keyof QualityProfilesContextProps>>>
> {
  function ComponentWithQualityProfilesProps(props: P) {
    const context = useOutletContext<QualityProfilesContextProps>();
    return <WrappedComponent {...props} {...context} />;
  }

  (
    ComponentWithQualityProfilesProps as React.FC<
      React.PropsWithChildren<React.PropsWithChildren<P>>
    >
  ).displayName = getWrappedDisplayName(WrappedComponent, 'withQualityProfilesContext');

  return ComponentWithQualityProfilesProps;
}

export function useQualityProfilesContext() {
  return useOutletContext<QualityProfilesContextProps>();
}
