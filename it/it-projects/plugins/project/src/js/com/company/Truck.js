var Truck = function(brand, model, year) {
	this.brand = brand;
	this.model = model;
	this.year = year;
	this.engineState = 'stopped';
	this.messageToDriver = '';
};

Truck.prototype = {

	getFullName : function() {
		return this.brand + ' ' + this.model + ' ' + 'Y: ' + this.year;
	},

	calculatePrice : function() {
		if (this.year < 1990) {
			return '$15000';
		} else if (this.year > 2011) {
			return '$300000';
		} else {
			return '$15000 - 300000';
		}
	}
};