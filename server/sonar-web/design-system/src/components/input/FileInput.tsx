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
import classNames from 'classnames';
import { useCallback, useRef, useState } from 'react';
import { ButtonSecondary } from '../../sonar-aligned/components/buttons/ButtonSecondary';
import { Note } from '../Text';

interface Props {
  chooseLabel: string;
  className?: string;
  clearLabel: string;
  id?: string;
  name?: string;
  noFileLabel: string;
  onFileSelected?: (file?: File) => void;
  required?: boolean;
}

export function FileInput(props: Readonly<Props>) {
  const { className, id, name, onFileSelected, required } = props;
  const { chooseLabel, clearLabel, noFileLabel } = props;

  const [selectedFileName, setSelectedFileName] = useState<string | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileInputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      onFileSelected?.(file);
      setSelectedFileName(file?.name);
    },
    [onFileSelected],
  );

  const handleFileInputReset = useCallback(() => {
    if (fileInputRef.current) {
      onFileSelected?.(undefined);
      fileInputRef.current.value = '';
      setSelectedFileName(undefined);
    }
  }, [fileInputRef, onFileSelected]);

  const handleFileInputClick = useCallback(() => {
    fileInputRef.current?.click();
  }, [fileInputRef]);

  return (
    <div className={classNames('sw-flex sw-items-center sw-gap-2', className)}>
      {selectedFileName ? (
        <>
          <ButtonSecondary onClick={handleFileInputReset}>{clearLabel}</ButtonSecondary>
          <Note>{selectedFileName}</Note>
        </>
      ) : (
        <>
          <ButtonSecondary onClick={handleFileInputClick}>{chooseLabel}</ButtonSecondary>
          <Note>{noFileLabel}</Note>
        </>
      )}
      <input
        data-testid="file-input"
        hidden
        id={id}
        name={name}
        onChange={handleFileInputChange}
        ref={fileInputRef}
        required={required}
        type="file"
      />
    </div>
  );
}
