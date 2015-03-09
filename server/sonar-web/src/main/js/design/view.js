/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define([
  'design/info-view',
  'templates/design'
], function (InfoView) {
  var $ = jQuery,
      API_DEPENDENCIES = baseUrl + '/api/dependencies';

  return Marionette.Layout.extend({
    template: Templates['design'],
    className: 'dsm',

    regions: {
      infoRegion: '.dsm-info'
    },

    ui: {
      titles: '.dsm-body-title',
      cells: '.dsm-body-cell',
      dependencies: '.dsm-body-dependency'
    },

    events: {
      'click @ui.titles': 'highlightComponent',
      'dblclick @ui.titles': 'goToComponent',
      'click @ui.cells': 'highlightCell',
      'dblclick @ui.dependencies': 'showDependencies',
      'change .js-hide-dir': 'toggleDirDisplay'
    },

    onRender: function () {
      this.toggleDirDisplay();
    },

    clearCells: function () {
      this.ui.titles.removeClass('dsm-body-highlighted dsm-body-usage dsm-body-dependency');
      this.ui.cells.removeClass('dsm-body-highlighted dsm-body-usage dsm-body-dependency');
    },

    highlightComponent: function (e) {
      var index = this.ui.titles.index($(e.currentTarget));
      this.clearCells();
      this.highlightRow(index);
      this.highlightColumn(index);
      this.highlightUsages(index);
      this.highlightDependencies(index);
    },

    highlightCell: function (e) {
      var cell = $(e.currentTarget),
          column = cell.parent().children().index(cell) - 1,
          row = cell.parent().parent().children().index(cell.parent());
      this.clearCells();
      if (row === column) {
        this.highlightRow(row);
        this.highlightColumn(row);
        this.highlightUsages(row);
        this.highlightDependencies(row);
      } else {
        this.highlightRow(column, 'dsm-body-usage');
        this.highlightColumn(column, 'dsm-body-usage');
        this.highlightRow(row, 'dsm-body-dependency');
        this.highlightColumn(row, 'dsm-body-dependency');
      }
    },

    highlightRow: function (index, c) {
      if (c == null) {
        c = 'dsm-body-highlighted';
      }
      this.$('.dsm-body').find('tr:eq(' + index + ')').find('td').addClass(c);
    },

    highlightColumn: function (index, c) {
      if (c == null) {
        c = 'dsm-body-highlighted';
      }
      this.$('.dsm-body tr').each(function () {
        return $(this).find('td:eq(' + (index + 1) + ')').addClass(c);
      });
    },

    highlightUsages: function (index) {
      var that = this;
      this.collection.at(index).get('v').forEach(function (d, i) {
        if (i < index && (d.w != null)) {
          that.$('tr:eq(' + i + ')').find('.dsm-body-title').addClass('dsm-body-usage');
        }
      });
    },

    highlightDependencies: function (index) {
      var that = this;
      this.collection.forEach(function (model, i) {
        if (model.get('v')[index].w != null) {
          that.$('tr:eq(' + i + ')').find('.dsm-body-title').addClass('dsm-body-dependency');
        }
      });
    },

    goToComponent: function (e) {
      var cell = $(e.currentTarget),
          row = cell.parent().parent().children().index(cell.parent()),
          model = this.collection.at(row),
          page = model.get('q') === 'CLA' || model.get('q') === 'FIL' ? 'dashboard' : 'design';
      window.location = baseUrl + '/' + page + '/index/' + model.get('i');
    },

    showDependencies: function (e) {
      var that = this,
          cell = $(e.currentTarget),
          column = cell.parent().children().index(cell) - 1,
          row = cell.parent().parent().children().index(cell.parent()),
          id = this.collection.at(row).get('v')[column].i;
      if (!id) {
        return;
      }
      return $.get(API_DEPENDENCIES, { parent: id }).done(function (data) {
        new InfoView({
          collection: new Backbone.Collection(data),
          first: that.collection.at(column).toJSON(),
          second: that.collection.at(row).toJSON()
        }).render();
      });
    },

    toggleDirDisplay: function () {
      var rows = this.$('tr');
      rows.each(function (index) {
        if ($(this).data('empty') != null) {
          $(this).toggleClass('hidden');
          rows.each(function () {
            $(this).find('td').eq(index + 1).toggleClass('hidden');
          });
        }
      });
    },

    serializeData: function () {
      var hasDirectories = this.collection.some(function (model) {
        return model.get('q') === 'DIR';
      });
      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        hasDirectories: hasDirectories
      });
    }
  });

});
