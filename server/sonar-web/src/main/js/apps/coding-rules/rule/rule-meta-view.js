import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import RuleFilterMixin from './rule-filter-mixin';
import '../templates';

export default Marionette.ItemView.extend(RuleFilterMixin).extend({
  template: Templates['coding-rules-rule-meta'],

  modelEvents: {
    'change': 'render'
  },

  ui: {
    tagsChange: '.coding-rules-detail-tags-change',
    tagInput: '.coding-rules-detail-tag-input',
    tagsEdit: '.coding-rules-detail-tag-edit',
    tagsEditDone: '.coding-rules-detail-tag-edit-done',
    tagsEditCancel: '.coding-rules-details-tag-edit-cancel',
    tagsList: '.coding-rules-detail-tag-list'
  },

  events: {
    'click @ui.tagsChange': 'changeTags',
    'click @ui.tagsEditDone': 'editDone',
    'click @ui.tagsEditCancel': 'cancelEdit',
    'click .js-rule-filter': 'onRuleFilterClick'
  },

  onRender: function () {
    this.$('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  requestTags: function () {
    var url = baseUrl + '/api/rules/tags';
    return $.get(url);
  },

  changeTags: function () {
    var that = this;
    this.requestTags().done(function (r) {
      that.ui.tagInput.select2({
        tags: _.difference(_.difference(r.tags, that.model.get('tags')), that.model.get('sysTags')),
        width: '300px'
      });

      that.ui.tagsEdit.removeClass('hidden');
      that.ui.tagsList.addClass('hidden');
      that.tagsBuffer = that.ui.tagInput.select2('val');
      that.ui.tagInput.select2('open');
    });
  },

  cancelEdit: function () {
    this.ui.tagsList.removeClass('hidden');
    this.ui.tagsEdit.addClass('hidden');
    if (this.ui.tagInput.select2) {
      this.ui.tagInput.select2('val', this.tagsBuffer);
      this.ui.tagInput.select2('close');
    }
  },

  editDone: function () {
    var that = this,
        tags = this.ui.tagInput.val();
    return $.ajax({
      type: 'POST',
      url: baseUrl + '/api/rules/update',
      data: {
        key: this.model.get('key'),
        tags: tags
      }
    }).done(function (r) {
      that.model.set('tags', r.rule.tags);
      that.cancelEdit();
    }).always(function () {
      that.cancelEdit();
    });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.app.canWrite,
      subCharacteristic: this.options.app.getSubCharacteristicName(this.model.get('debtSubChar')),
      allTags: _.union(this.model.get('sysTags'), this.model.get('tags')),
      permalink: baseUrl + '/coding_rules#rule_key=' + encodeURIComponent(this.model.id)
    });
  }
});


