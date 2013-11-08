/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var Filter = Backbone.Model.extend({});



  var Filters = Backbone.Collection.extend({
    model: Filter
  });



  var BaseFilterView = Backbone.Marionette.ItemView.extend({
    template: '#filterTemplate',
    className: 'navigator-filter',


    events: function() {
      return {};
    },


    modelEvents: {
      "change:enabled": "render"
    },


    initialize: function() {
      Backbone.Marionette.ItemView.prototype.initialize.call(this, arguments);
      this.model.view = this;
    },


    render: function() {
      Backbone.Marionette.ItemView.prototype.render.call(this, arguments);

      this.$el.toggleClass(
          'navigator-filter-disabled',
          !this.model.get('enabled'));
    },


    renderBody: function() {
      return '';
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        body: this.renderBody()
      });
    },


    restore: function() {}
  });



  var SelectFilterView = BaseFilterView.extend({

    renderBody: function() {
      var template = _.template($j('#selectFilterTemplate').html());
      return template(this.model.toJSON());
    },


    onDomRefresh: function() {
      var that = this;

      this.$('.navigator-filter-label').hide();

      this.$(':input').select2({
        allowClear: false,
        placeholder: this.model.get('name'),
        width: '150px'
      }).on('change', function(e) {
            that.model.set('value', e.val);
          });
    },


    restore: function() {
      if (this.model.get('value')) {
        this.$(':input').select2('val', this.model.get('value'));
      }
    },


    focus: function() {}
  });



  var AjaxSelectFilterView = BaseFilterView.extend({

    renderBody: function() {
      var template = _.template($j('#ajaxSelectFilterTemplate').html());
      return template(this.model.toJSON());
    },

    onDomRefresh: function() {
      var that = this;

      this.$('.navigator-filter-label').hide();

      this.$(':input').select2(_.extend({
        allowClear: false,
        placeholder: this.model.get('name'),
        width: '150px',
        minimumInputLength: 2
      }, this.model.get('select2')))
          .on('change', function(e) {
            that.model.set('value', e.val);
          });
    },

    restore: function() {
      if (this.model.get('value')) {
        this.$(':input').select2('data', this.model.get('value'));
      }
    },

    focus: function() {}
  });



  var RangeFilterView = BaseFilterView.extend({

    events: function() {
      return _.extend(BaseFilterView.prototype.events.call(), {
        'change input': 'changeInput'
      });
    },


    renderBody: function() {
      var template = _.template($j('#rangeFilterTemplate').html());
      return template(this.model.toJSON());
    },


    onRender: function() {
      this.inputFrom = this.$('[name="' + this.model.get('propertyFrom') + '"]');
      this.inputTo = this.$('[name="' + this.model.get('propertyTo') + '"]');
    },


    changeInput: function() {
      this.model.set('value', {
        from: this.inputFrom.val(),
        to: this.inputTo.val()
      });
    },


    restore: function() {
      var value = this.model.get('value');
      if (typeof value === 'object') {
        this.inputFrom.val(value.from || '');
        this.inputTo.val(value.to || '');
      }
    },


    focus: function() {
      this.inputFrom.focus();
    }

  });


  var FilterBarView = Backbone.Marionette.CompositeView.extend({
    template: '#filterBarTemplate',
    itemViewContainer: '.navigator-filters-list',


    collectionEvents: {
      'change:value': 'changeFilters',
      'change:enabled': 'renderDisabledFilters'
    },


    ui: {
      disabledFilters: '.navigator-disabled-filters'
    },


    getItemView: function(item) {
      return item.get('type') || BaseFilterView;
    },


    itemViewOptions: function() {
      return {
        filterBarView: this
      };
    },


    render: function() {
      Backbone.Marionette.CompositeView.prototype.render.call(this, arguments);
      this.renderDisabledFilters();
    },


    renderDisabledFilters: function() {
      var that = this,
          disabledFilters = this.collection.where({ enabled: false });

      that.ui.disabledFilters.select2('destroy').empty().show();

      if (disabledFilters.length > 0) {
        $j('<option>').appendTo(that.ui.disabledFilters);
        _.each(disabledFilters, function(item) {
          $j('<option>').text(item.get('name')).prop('value', item.cid)
              .appendTo(that.ui.disabledFilters);
        });
        that.ui.disabledFilters.select2({
          allowClear: true,
          placeholder: 'More criteria',
          width: '150px'
        }).on('change', function(e) {
              that.ui.disabledFilters.select2('val', '');
              that.enableFilter(e.val);
            });
      } else {
        that.ui.disabledFilters.hide();
      }
    },


    enableFilter: function(key) {
      var item = this.collection.find(function(item) {
        return item.cid === key;
      });

      if (item) {
        item.view.$el.detach().appendTo(this.itemViewContainer);
        item.set('enabled', true);
        item.view.focus();
      }
    },


    changeFilters: function() {
      var query = {};
      this.collection.each(function(item) {
        if (item.get('value')) {
          query[item.get('property')] = item.get('value');
        }
      });
      this.applyQuery($j.param(query));
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
    FilterBarView: FilterBarView,
    SelectFilterView: SelectFilterView,
    AjaxSelectFilterView: AjaxSelectFilterView,
    RangeFilterView: RangeFilterView
  });

})();
