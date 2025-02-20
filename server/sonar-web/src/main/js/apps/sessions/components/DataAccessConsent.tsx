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

import { FormattedMessage } from 'react-intl';
import { Modal } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { SafeHTMLInjection, SanitizeLevel, CenteredLayout, HtmlFormatter } from '~design-system';
import { noop } from 'lodash';

export interface DataAccessConsentProps {
  message: string;
}

function hasUserAlreadyAcknowledged() {
  if (sessionStorage.getItem("us-data-protection-acknowledged") != undefined
      && sessionStorage.getItem("us-data-protection-acknowledged").length > 0) {
    return sessionStorage.getItem("us-data-protection-acknowledged");
  }
  return  false;
}

export default function DataAccessConsent({
    message,
  }: Readonly<DataAccessConsentProps>) {
  const [isOpen, setIsOpen] = React.useState(!hasUserAlreadyAcknowledged());

  const acceptConsent = () => {
    sessionStorage.setItem("us-data-protection-acknowledged", "true");
    setIsOpen(false);
  };

  return (
    <>
      {isOpen && (
          <Modal onClose={noop} closeOnOverlayClick={false} >
            <Modal.Body>
              <HtmlFormatter>
                <SafeHTMLInjection htmlAsString={message}/>
              </HtmlFormatter>
            </Modal.Body>
            <Modal.Footer
              primaryButton={
                <Button onClick={acceptConsent} variety={ButtonVariety.Primary}>
                  <FormattedMessage id="login.us_data_protection_consent.button_primary" />
                </Button>
              }
              secondaryButton={null}
            />
          </Modal>
      )}
    </>
  );
}