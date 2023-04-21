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

import classNames from 'classnames';
import React from 'react';
import { Button } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  showAllFilters: boolean;
  onClick: (val: boolean) => void;
}

export default function FiltersVisibilityButton(props: Props) {
  const { showAllFilters } = props;

  return (
    <div className="display-flex-justify-center spacer-top">
      <Button
        onClick={() => props.onClick(!showAllFilters)}
        className={classNames({ it__show_more_facets: !showAllFilters })}
      >
        {translate(showAllFilters ? 'issues.show_less_filters' : 'issues.show_more_filters')}
      </Button>
    </div>
  );
}
