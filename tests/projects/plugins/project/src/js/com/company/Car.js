var Car = function(brand, model, year) {
	this.brand = brand;
	this.model = model;
	this.year = year;
	this.engineState = 'stopped';
	this.messageToDriver = '';
};

Car.prototype = {

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

	startEngine : function() {
		this.engineState = 'started';
		return 'engine started';
	},

	stopEngine : function() {
		this.engineState = 'stopped';
		return 'engine stopped';
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