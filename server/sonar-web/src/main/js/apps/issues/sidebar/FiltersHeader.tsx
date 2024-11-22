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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { BasicSeparator, PageTitle } from '~design-system';
import ModeBanner from '../../../components/common/ModeBanner';
import { translate } from '../../../helpers/l10n';

interface Props {
  displayReset: boolean;
  onReset: () => void;
}

export function FiltersHeader({ displayReset, onReset }: Props) {
  return (
    <div className="sw-mb-5">
      <div className="sw-flex sw-h-9 sw-items-center sw-justify-between">
        <PageTitle className="sw-typo-lg-semibold" as="h2" text={translate('filters')} />

        {displayReset && (
          <Button onClick={onReset} variety={ButtonVariety.DangerOutline}>
            {translate('clear_all_filters')}
          </Button>
        )}
      </div>

      <ModeBanner as="facetBanner" />
      <BasicSeparator className="sw-mt-4" />
    </div>
  );
}
