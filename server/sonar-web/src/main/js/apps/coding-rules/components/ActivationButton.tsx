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
import * as React from 'react';
import { Profile as BaseProfile } from '../../../api/quality-profiles';
import { Rule, RuleActivation, RuleDetails } from '../../../types/types';
import ActivationFormModal from './ActivationFormModal';

interface Props {
  activation?: RuleActivation;
  ariaLabel?: string;
  buttonText: string;
  className?: string;
  modalHeader: string;
  onDone?: (severity: string, prioritizedRule: boolean) => Promise<void> | void;
  profiles: BaseProfile[];
  rule: Rule | RuleDetails;
}

export default function ActivationButton(props: Props) {
  const { className, ariaLabel, buttonText, activation, modalHeader, profiles, rule } = props;
  const [modalOpen, setModalOpen] = React.useState(false);

  return (
    <>
      <Button
        variety={ButtonVariety.Default}
        aria-label={ariaLabel}
        className={className}
        id="coding-rules-quality-profile-activate"
        onClick={() => setModalOpen(true)}
      >
        {buttonText}
      </Button>

      <ActivationFormModal
        activation={activation}
        modalHeader={modalHeader}
        isOpen={modalOpen}
        onOpenChange={setModalOpen}
        onClose={() => setModalOpen(false)}
        onDone={props.onDone}
        profiles={profiles}
        rule={rule}
      />
    </>
  );
}
