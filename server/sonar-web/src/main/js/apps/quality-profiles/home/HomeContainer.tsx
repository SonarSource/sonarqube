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

import { useOutletContext, useSearchParams } from 'react-router-dom';
import { QualityProfilesContextProps } from '../qualityProfilesContext';
import Evolution from './Evolution';
import LanguageSelect from './LanguageSelect';
import PageHeader from './PageHeader';
import ProfilesList from './ProfilesList';

export default function HomeContainer() {
  const context = useOutletContext<QualityProfilesContextProps>();
  const [searchParams] = useSearchParams();

  const selectedLanguage = searchParams.get('language') ?? undefined;

  return (
    <div>
      <PageHeader {...context} />

      <div className="sw-grid sw-grid-cols-3 sw-gap-12 sw-mt-12">
        <main className="sw-col-span-2">
          <LanguageSelect currentFilter={selectedLanguage} languages={context.languages} />
          <ProfilesList {...context} language={selectedLanguage} />
        </main>
        <aside>
          <Evolution {...context} />
        </aside>
      </div>
    </div>
  );
}
