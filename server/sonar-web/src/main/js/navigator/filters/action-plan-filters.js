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
  'navigator/filters/base-filters',
  'navigator/filters/choice-filters'
], function (BaseFilters, ChoiceFilters) {

  return ChoiceFilters.ChoiceFilterView.extend({

    initialize: function() {
      ChoiceFilters.ChoiceFilterView.prototype.initialize.apply(this, arguments);
      this.projectFilter = this.model.get('projectFilter');
      this.listenTo(this.projectFilter, 'change:value', this.onChangeProjectFilter);
      this.onChangeProjectFilter();
    },


    onChangeProjectFilter: function() {
      var that = this;
      _.each(that.model.get('choices'), function(v, k) {
        that.choices.add(new Backbone.Model({ id: k, text: v }));
      });

      var projects = this.projectFilter.get('value');
      if (_.isArray(projects) && projects.length === 1) {
        return this.fetchActionPlans(projects[0]);
      } else {
        return this.makeInactive();
      }
    },


    showDetails: function() {
      if (!this.$el.is('.navigator-filter-inactive')) {
        ChoiceFilters.ChoiceFilterView.prototype.showDetails.apply(this, arguments);
      }
    },


    makeActive: function() {
      this.model.set({
        inactive: false,
        title: ''
      });
      this.model.trigger('change:enabled');
      this.$el.removeClass('navigator-filter-inactive').prop('title', '');
    },


    makeInactive: function() {
      this.model.set({
        inactive: true,
        title: window.SS.phrases.actionPlanNotAvailable
      });
      this.model.trigger('change:enabled');
      this.choices.reset([]);
      this.detailsView.updateLists();
      this.detailsView.updateValue();
      this.$el.addClass('navigator-filter-inactive')
          .prop('title', window.SS.phrases.actionPlanNotAvailable);
    },


    fetchActionPlans: function(project) {
      var that = this;
      return jQuery.ajax({
        url: baseUrl + '/api/action_plans/search',
        type: 'GET',
        data: { project: project }
      }).done(function(r) {
            var nonClosedActionPlans =
                _.sortBy(_.reject(r.actionPlans, function(plan) {
                      return plan.status === 'CLOSED';
                    }), 'name');

            that.choices.reset(nonClosedActionPlans.map(function(plan) {
              return {
                id: plan.key,
                text: plan.name,
                category: plan.fDeadLine
              };
            }));
            _.each(that.model.get('choices'), function(v, k) {
              that.choices.add(new Backbone.Model({ id: k, text: v }));
            });
            var value = that.model.get('value');
            _.each(value, function(v) {
              var cModel = that.choices.findWhere({ id: v });
              cModel.set('checked', true);
            });
            that.detailsView.updateValue();
            that.render();

            that.makeActive();
          });
    },


    restore: function(value) {
      if (_.isString(value)) {
        value = value.split(',');
      }

      if (this.choices && value.length > 0) {
        this.model.set({ value: value, enabled: true });
        this.onChangeProjectFilter();
      } else {
        this.clear();
      }
    }

  });

});
