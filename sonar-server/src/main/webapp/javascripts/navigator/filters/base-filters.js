/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var Filter = Backbone.Model.extend({});



  var Filters = Backbone.Collection.extend({
    model: Filter
  });



  var DetailsFilterView = Backbone.Marionette.ItemView.extend({
    template: '#detailsFilterTemplate',
    className: 'navigator-filter-details',

    onShow: function() {},
    onHide: function() {}
  });



  var BaseFilterView = Backbone.Marionette.ItemView.extend({
    template: '#baseFilterTemplate',
    className: 'navigator-filter',


    events: function() {
      return {
        'click': 'toggleDetails'
      };
    },


    modelEvents: {
      'change:enabled': 'focus',
      'change:filters': 'render' // for more criteria filter
    },


    initialize: function(options) {
      Backbone.Marionette.ItemView.prototype.initialize.apply(this, arguments);

      var detailsView = options.detailsView || DetailsFilterView;
      this.detailsView = new detailsView({
        model: this.model
      });
      this.detailsView.render = this.renderDetails;

      this.model.view = this;

      this.listenTo(this.model, 'change:value', this.renderBase);
    },


    attachDetailsView: function() {
      this.detailsView.$el.detach().appendTo($j('body'));
    },


    render: function() {
      this.renderBase();

      this.attachDetailsView();
      this.detailsView.render.call(this.detailsView);

      this.$el.toggleClass(
          'navigator-filter-disabled',
          !this.model.get('enabled'));
    },


    renderBase: function() {
      Backbone.Marionette.ItemView.prototype.render.apply(this, arguments);
    },


    focus: function() {
      this.render();
      this.showDetails();
    },


    toggleDetails: function(e) {
      e.stopPropagation();
      if (this.$el.hasClass('active')) {
        this.hideDetails();
      } else {
        this.showDetails();
      }
    },


    showDetails: function() {
      this.registerShowedDetails();

      var top = this.$el.offset().top + this.$el.outerHeight(),
          left = this.$el.offset().left;

      this.detailsView.$el.css({ top: top, left: left }).addClass('active');
      this.$el.addClass('active');
      $j('body').addClass('navigator-filter-shown');
      this.detailsView.onShow();
    },


    registerShowedDetails: function() {
      this.options.filterBarView.hideDetails();
      this.options.filterBarView.showedView = this;
    },


    hideDetails: function() {
      this.detailsView.$el.removeClass('active');
      this.$el.removeClass('active');
      $j('body').removeClass('navigator-filter-shown');
      this.detailsView.onHide();
    },


    renderValue: function() {
      return this.model.get('value') || 'unset';
    },


    renderDetails: function() {
      Backbone.Marionette.ItemView.prototype.render.apply(this, arguments);
    },


    isDefaultValue: function() {
      return true;
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        value: this.renderValue(),
        defaultValue: this.isDefaultValue()
      });
    }

  });



  var FilterBarView = Backbone.Marionette.CompositeView.extend({
    template: '#filterBarTemplate',
    itemViewContainer: '.navigator-filters-list',


    collectionEvents: {
      'change:value': 'changeFilters'
    },


    getItemView: function(item) {
      return item.get('type') || BaseFilterView;
    },


    itemViewOptions: function() {
      return {
        filterBarView: this
      };
    },


    initialize: function() {
      Backbone.Marionette.CompositeView.prototype.initialize.apply(this, arguments);

      var that = this;
      $('body').on('click', function() {
        that.hideDetails();
      });

      var disabledFilters = this.collection.where({ enabled: false });
      this.moreCriteriaFilter = new window.SS.Filter({
        type: window.SS.MoreCriteriaFilterView,
        enabled: true,
        filters: disabledFilters
      });
      this.collection.add(this.moreCriteriaFilter);
    },


    render: function() {
      Backbone.Marionette.CompositeView.prototype.render.apply(this, arguments);
    },


    hideDetails: function() {
      if (_.isObject(this.showedView)) {
        this.showedView.hideDetails();
      }
    },


    enableFilter: function(id) {
      var filter = this.collection.get(id),
          filterView = filter.view;

      filterView.$el.detach().insertBefore(this.$('.navigator-filter-more-criteria'));
      filter.set('enabled', 'true');

      var disabledFilters = this.collection.where({ enabled: false });
      if (disabledFilters.length === 0) {
        this.moreCriteriaFilter.set({ enabled: false }, { silent: true });
      }
      this.moreCriteriaFilter.set('filters', disabledFilters);
    },


    changeFilters: function() {
      var query = _.clone(this.options.extra);
      this.collection.each(function(item) {
        var value = item.get('value');

        if (value) {
          if (_.isObject(value) && !_.isArray(value)) {
            _.extend(query, value);
          } else {
            query[item.get('property')] = item.get('value');
          }
        }

      });
      this.applyQuery($j.param(query, true));
    },


    applyQuery: function(query) {
      $j.ajax({
        url: baseUrl + '/issues/search',
        type: 'get',
        data: query
      }).done(function(r) {
            $j('.navigator-results').html(r);
          });
    }
  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    Filter: Filter,
    Filters: Filters,
    BaseFilterView: BaseFilterView,
    DetailsFilterView: DetailsFilterView,
    FilterBarView: FilterBarView
  });

})();
