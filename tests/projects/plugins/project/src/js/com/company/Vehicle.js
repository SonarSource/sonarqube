/**
 * This is copy/paste file from Car.js with couple methods removed
 * 
 * Removed methods:
 * - startEngine ()
 * - stopEngine ()
 * 
 */
var Vehicle = function(brand, model, year) {
	this.brand = brand;
	this.model = model;
	this.year = year;
	this.engineState = 'stopped';
	this.messageToDriver = '';
};


// single line comments line 1
// single line comments line 2
// single line comments line 3
// single line comments line 4
Vehicle.prototype = {

	getFullName : function() {
		return this.brand + ' ' + this.model + ' ' + 'Y: ' + this.year;
	},

	calculatePrice : function() {
		if (this.year < 1990) {
			return '$1500';
		} else if (this.year > 2011) {
			return '$30000';
		} else {
			return '$1500 - 30000';
		}
	},

	stopEngineWithCheck : function() {
		if (this.engineState === 'started') {
			this.engineState = 'stopped';
			this.messageToDriver = 'all good. c u later';
			return 'engine stopped';
		} else {
			this.messageToDriver = 'engine not started. what do you want me to do?';
			return 'engine was not running';
		}
	},

	tuneCar : function() {
		this.year = '2011';
	}

};