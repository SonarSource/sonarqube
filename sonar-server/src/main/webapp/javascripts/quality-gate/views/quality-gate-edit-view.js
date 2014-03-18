(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'handlebars'], function(Marionette, Handlebars) {
    var QualityGateEditView;
    return QualityGateEditView = (function(_super) {
      __extends(QualityGateEditView, _super);

      function QualityGateEditView() {
        return QualityGateEditView.__super__.constructor.apply(this, arguments);
      }

      QualityGateEditView.prototype.className = 'modal';

      QualityGateEditView.prototype.template = Handlebars.compile(jQuery('#quality-gate-edit-template').html());

      QualityGateEditView.prototype.ui = {
        nameInput: '#quality-gate-edit-name'
      };

      QualityGateEditView.prototype.events = {
        'submit form': 'onSubmit',
        'click #quality-gate-cancel-create': 'hide'
      };

      QualityGateEditView.prototype.onRender = function() {
        return this.$el.dialog({
          dialogClass: 'no-close',
          width: '600px',
          draggable: false,
          autoOpen: false,
          modal: true,
          minHeight: 50,
          resizable: false,
          title: null
        });
      };

      QualityGateEditView.prototype.show = function() {
        this.render();
        this.$el.dialog('open');
        return this.ui.nameInput.focus();
      };

      QualityGateEditView.prototype.hide = function() {
        return this.$el.dialog('close');
      };

      QualityGateEditView.prototype.saveRequest = function(data) {
        return jQuery.ajax({
          type: 'POST',
          url: "" + baseUrl + "/api/qualitygates/" + this.method,
          data: data
        }).done((function(_this) {
          return function() {
            return _this.hide();
          };
        })(this));
      };

      QualityGateEditView.prototype.onSubmit = function(e) {
        e.preventDefault();
        switch (this.method) {
          case 'create':
            return this.createQualityGate();
          case 'copy':
            return this.copyQualityGate();
          case 'rename':
            return this.saveQualityGate();
        }
      };

      QualityGateEditView.prototype.createQualityGate = function() {
        var data;
        data = {
          name: this.ui.nameInput.val()
        };
        return this.saveRequest(data).done((function(_this) {
          return function(r) {
            _this.model.set({
              id: r.id,
              name: r.name
            });
            _this.options.app.qualityGates.add(_this.model);
            return _this.options.app.router.navigate("show/" + r.id, {
              trigger: true
            });
          };
        })(this));
      };

      QualityGateEditView.prototype.saveQualityGate = function() {
        var data;
        data = {
          id: this.model.id,
          name: this.ui.nameInput.val()
        };
        return this.saveRequest(data).done((function(_this) {
          return function(r) {
            return _this.model.set({
              name: r.name
            });
          };
        })(this));
      };

      QualityGateEditView.prototype.copyQualityGate = function() {
        var data;
        data = {
          id: this.model.id,
          name: this.ui.nameInput.val()
        };
        return this.saveRequest(data).done((function(_this) {
          return function(r) {
            _this.model.set({
              id: r.id,
              name: r.name
            });
            _this.options.app.qualityGates.add(_this.model);
            return _this.options.app.router.navigate("show/" + r.id, {
              trigger: true
            });
          };
        })(this));
      };

      QualityGateEditView.prototype.serializeData = function() {
        if (this.model) {
          return _.extend(this.model.toJSON(), {
            method: this.method
          });
        }
      };

      return QualityGateEditView;

    })(Marionette.ItemView);
  });

}).call(this);
