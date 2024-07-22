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
import { CardWithPrimaryBackground, SubHeadingHighlight } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import ModalButton from '../../../components/controls/ModalButton';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';

interface Props {
  isOptimizing?: boolean;
  renderCaycModal: ({ onClose }: { onClose: () => void }) => React.ReactNode;
}

export default function CaycNonCompliantBanner({ renderCaycModal, isOptimizing }: Readonly<Props>) {
  return (
    <CardWithPrimaryBackground className="sw-mb-9 sw-p-8">
      <SubHeadingHighlight className="sw-mb-2">
        {translate(
          isOptimizing
            ? 'quality_gates.cayc_optimize.banner.title'
            : 'quality_gates.cayc_missing.banner.title',
        )}
      </SubHeadingHighlight>
      <div>
        <FormattedMessage
          id={
            isOptimizing
              ? 'quality_gates.cayc_optimize.banner.description'
              : 'quality_gates.cayc_missing.banner.description'
          }
          values={{
            cayc_link: (
              <DocumentationLink to={DocLink.CaYC}>
                {translate('quality_gates.cayc')}
              </DocumentationLink>
            ),
          }}
        />
      </div>
      <ModalButton modal={renderCaycModal}>
        {({ onClick }) => (
          <Button className="sw-mt-4" onClick={onClick} variety={ButtonVariety.Primary}>
            {translate(
              isOptimizing
                ? 'quality_gates.cayc_condition.review_optimize'
                : 'quality_gates.cayc_condition.review_update',
            )}
          </Button>
        )}
      </ModalButton>
    </CardWithPrimaryBackground>
  );
}
