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
import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from '../../../components/common/modal-form';
import Template from '../templates/quality-profiles-create-profile.hbs';
import { createQualityProfile } from '../../../api/quality-profiles';

export default ModalFormView.extend({
  template: Template,

  events () {
    return _.extend(ModalFormView.prototype.events.apply(this, arguments), {
      'change #create-profile-language': 'onLanguageChange'
    });
  },

  onFormSubmit () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);

    const form = this.$('form')[0];
    const data = new FormData(form);

    createQualityProfile(data)
        .then(r => {
          this.trigger('done', r.profile);
          this.destroy();
        })
        .catch(e => {
          e.response.json().then(r => this.showErrors(r.errors, r.warnings));
        });
  },

  onRender () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('select').select2({
      width: '250px',
      minimumResultsForSearch: 50
    });
    this.onLanguageChange();
  },

  onLanguageChange () {
    const that = this;
    const language = this.$('#create-profile-language').val();
    const importers = this.getImportersForLanguages(language);
    this.$('.js-importer').each(function () {
      that.emptyInput($(this));
      $(this).addClass('hidden');
    });
    importers.forEach(function (importer) {
      that.$(`.js-importer[data-key="${importer.key}"]`).removeClass('hidden');
    });
  },

  emptyInput (e) {
    e.wrap('<form>').closest('form').get(0).reset();
    e.unwrap();
  },

  getImportersForLanguages (language) {
    if (language != null) {
      return this.options.importers.filter(function (importer) {
        return importer.languages.indexOf(language) !== -1;
      });
    } else {
      return [];
    }
  },

  serializeData () {
    return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
      languages: this.options.languages,
      importers: this.options.importers
    });
  }
});

