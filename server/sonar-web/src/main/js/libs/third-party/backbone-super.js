// This is a plugin, constructed from parts of Backbone.js and John Resig's inheritance script.
// (See http://backbonejs.org, http://ejohn.org/blog/simple-javascript-inheritance/)
// No credit goes to me as I did absolutely nothing except patch these two together.
(function(root, factory) {

  // Set up Backbone appropriately for the environment. Start with AMD.
  if (typeof define === 'function' && define.amd) {
    define(['underscore', 'backbone'], function(_, Backbone) {
      // Export global even in AMD case in case this script is loaded with
      // others that may still expect a global Backbone.
      factory( _, Backbone);
    });

  // Next for Node.js or CommonJS.
  } else if (typeof exports !== 'undefined' && typeof require === 'function') {
    var _ = require('underscore'),
		Backbone = require('backbone');
    factory(_, Backbone);

  // Finally, as a browser global.
  } else {
    factory(root._, root.Backbone);
  }

}(this, function factory(_, Backbone) {
	Backbone.Model.extend = Backbone.Collection.extend = Backbone.Router.extend = Backbone.View.extend = function(protoProps, classProps) {
		var child = inherits(this, protoProps, classProps);
		child.extend = this.extend;
		return child;
	};
	var unImplementedSuper = function(method){throw "Super does not implement this method: " + method;};

  var fnTest = /\b_super\b/;

  var makeWrapper = function(parentProto, name, fn) {
    var wrapper = function() {
      var tmp = this._super;

      // Add a new ._super() method that is the same method
      // but on the super-class
      this._super = parentProto[name] || unImplementedSuper(name);

      // The method only need to be bound temporarily, so we
      // remove it when we're done executing
      var ret;
      try {
        ret = fn.apply(this, arguments);
      } finally {
        this._super = tmp;
      }
      return ret;
    };

    //we must move properties from old function to new
    for (var prop in fn) {
      wrapper[prop] = fn[prop];
      delete fn[prop];
    }

    return wrapper;
  };

	var ctor = function(){}, inherits = function(parent, protoProps, staticProps) {
        var child, parentProto = parent.prototype;

		// The constructor function for the new subclass is either defined by you
		// (the "constructor" property in your `extend` definition), or defaulted
		// by us to simply call the parent's constructor.
		if (protoProps && protoProps.hasOwnProperty('constructor')) {
			child = protoProps.constructor;
		} else {
			child = function(){ return parent.apply(this, arguments); };
		}

		// Inherit class (static) properties from parent.
		_.extend(child, parent, staticProps);

		// Set the prototype chain to inherit from `parent`, without calling
		// `parent`'s constructor function.
		ctor.prototype = parentProto;
		child.prototype = new ctor();

		// Add prototype properties (instance properties) to the subclass,
		// if supplied.
		if (protoProps) {
			_.extend(child.prototype, protoProps);

			// Copy the properties over onto the new prototype
			for (var name in protoProps) {
				// Check if we're overwriting an existing function
				if (typeof protoProps[name] == "function" && fnTest.test(protoProps[name])) {
					child.prototype[name] = makeWrapper(parentProto, name, protoProps[name]);
				}
			}
		}

		// Add static properties to the constructor function, if supplied.
		if (staticProps) _.extend(child, staticProps);

		// Correctly set child's `prototype.constructor`.
		child.prototype.constructor = child;

		// Set a convenience property in case the parent's prototype is needed later.
		child.__super__ = parentProto;

		return child;
	};

	return inherits;
}));

