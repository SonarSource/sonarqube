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
import { getAlmSettings } from '../../../api/alm-settings';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import Link from '../../../components/common/Link';
import { Button } from '../../../components/controls/buttons';
import Dropdown from '../../../components/controls/Dropdown';
import DropdownIcon from '../../../components/icons/DropdownIcon';
import EllipsisIcon from '../../../components/icons/EllipsisIcon';
import { IMPORT_COMPATIBLE_ALMS } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { Permissions } from '../../../types/permissions';
import { LoggedInUser } from '../../../types/users';
import ProjectCreationMenuItem from './ProjectCreationMenuItem';

interface Props {
  className?: string;
  currentUser: LoggedInUser;
}

interface State {
  boundAlms: Array<string>;
}

const almSettingsValidators = {
  [AlmKeys.Azure]: (settings: AlmSettingsInstance) => !!settings.url,
  [AlmKeys.BitbucketServer]: (_: AlmSettingsInstance) => true,
  [AlmKeys.BitbucketCloud]: (_: AlmSettingsInstance) => true,
  [AlmKeys.GitHub]: (_: AlmSettingsInstance) => true,
  [AlmKeys.GitLab]: (settings: AlmSettingsInstance) => !!settings.url,
};

export class ProjectCreationMenu extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { boundAlms: [] };

  componentDidMount() {
    this.mounted = true;

    this.fetchAlmBindings();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  almSettingIsValid = (settings: AlmSettingsInstance) => {
    return almSettingsValidators[settings.alm](settings);
  };

  fetchAlmBindings = async () => {
    const { currentUser } = this.props;
    const canCreateProject = hasGlobalPermission(currentUser, Permissions.ProjectCreation);

    // getAlmSettings requires branchesEnabled
    if (!canCreateProject) {
      return;
    }

    const almSettings: AlmSettingsInstance[] = await getAlmSettings().catch(() => []);

    const boundAlms = IMPORT_COMPATIBLE_ALMS.filter((key) => {
      const currentAlmSettings = almSettings.filter((s) => s.alm === key);
      return (
        currentAlmSettings.length > 0 &&
        key === currentAlmSettings[0].alm &&
        this.almSettingIsValid(currentAlmSettings[0])
      );
    });

    if (this.mounted) {
      this.setState({
        boundAlms,
      });
    }
  };

  render() {
    const { className, currentUser } = this.props;
    const { boundAlms } = this.state;

    const canCreateProject = hasGlobalPermission(currentUser, Permissions.ProjectCreation);

    if (!canCreateProject) {
      return null;
    }

    return (
      <Dropdown
        className={className}
        onOpen={this.fetchAlmBindings}
        overlay={
          <ul className="menu">
            {[...boundAlms, 'manual'].map((alm) => (
              <li className="little-spacer-bottom" key={alm}>
                <ProjectCreationMenuItem alm={alm} />
              </li>
            ))}
            {boundAlms.length < IMPORT_COMPATIBLE_ALMS.length && (
              <li className="bordered-top little-padded-top">
                <Link className="display-flex-center" to={{ pathname: '/projects/create' }}>
                  <EllipsisIcon className="spacer-right" size={16} />
                  {translate('more')}
                </Link>
              </li>
            )}
          </ul>
        }
      >
        <Button className="button-primary">
          {translate('projects.add')}
          <DropdownIcon className="spacer-left " />
        </Button>
      </Dropdown>
    );
  }
}

export default withCurrentUserContext(ProjectCreationMenu);
