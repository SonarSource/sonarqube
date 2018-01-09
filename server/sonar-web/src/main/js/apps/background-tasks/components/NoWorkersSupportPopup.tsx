/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import BubblePopup from '../../../components/common/BubblePopup';
import { translate } from '../../../helpers/l10n';

interface Props {
  popupPosition?: any;
}

export default function NoWorkersSupportPopup(props: Props) {
  return (
    <BubblePopup position={props.popupPosition} customClass="bubble-popup-bottom-right">
      <div className="abs-width-400">
        <h6 className="spacer-bottom">{translate('background_tasks.add_more_workers')}</h6>
        <p className="big-spacer-bottom markdown">
          {translate('background_tasks.add_more_workers.text')}
        </p>
        <p>
          <a href="https://redirect.sonarsource.com/editions/enterprise.html" target="_blank">
            {translate('learn_more')}
          </a>
        </p>
      </div>
    </BubblePopup>
  );
}
