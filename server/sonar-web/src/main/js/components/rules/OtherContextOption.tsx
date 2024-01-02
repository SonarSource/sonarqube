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
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import Link from '../common/Link';
import CheckIcon from '../icons/CheckIcon';
import ClearIcon from '../icons/ClearIcon';

export default function OtherContextOption() {
  return (
    <>
      <h2>{translate('coding_rules.context.others.title')}</h2>
      <p>{translate('coding_rules.context.others.description.first')}</p>
      <p>{translate('coding_rules.context.others.description.second')}</p>
      <p>
        <span className="display-block big-spacer-left">
          <CheckIcon className="spacer-right" fill="green" />
          {translate('coding_rules.context.others.description.do')}
        </span>
        <span className="display-block big-spacer-left">
          <ClearIcon className="spacer-right" fill="red" />
          {translate('coding_rules.context.others.description.dont')}
        </span>
      </p>
      <h2>{translate('coding_rules.context.others.title_feedback')}</h2>
      <p>{translate('coding_rules.context.others.feedback_description_1')}</p>
      <Link
        to="https://portal.productboard.com/sonarsource/3-sonarqube/submit-idea"
        target="_blank"
      >
        {translate('coding_rules.context.others.feedback_description.link')}
      </Link>
      <p>{translate('coding_rules.context.others.feedback_description_2')}</p>
    </>
  );
}
