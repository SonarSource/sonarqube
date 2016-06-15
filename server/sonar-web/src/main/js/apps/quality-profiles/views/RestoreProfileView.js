/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import ModalFormView from '../../../components/common/modal-form';
import Template from '../templates/quality-profiles-restore-profile.hbs';
import { restoreQualityProfile } from '../../../api/quality-profiles';

export default ModalFormView.extend({
  template: Template,

  onFormSubmit (e) {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    const data = new FormData(e.currentTarget);

    this.disableForm();

    restoreQualityProfile(data)
        .then(r => {
          this.profile = r.profile;
          this.ruleSuccesses = r.ruleSuccesses;
          this.ruleFailures = r.ruleFailures;
          this.render();
          this.trigger('done');
        })
        .catch(e => {
          this.enableForm();
          e.response.json().then(r => this.showErrors(r.errors, r.warnings));
        });
  },

  serializeData() {
    return {
      ...ModalFormView.prototype.serializeData.apply(this, arguments),
      profile: this.profile,
      ruleSuccesses: this.ruleSuccesses,
      ruleFailures: this.ruleFailures
    };
  }
});
