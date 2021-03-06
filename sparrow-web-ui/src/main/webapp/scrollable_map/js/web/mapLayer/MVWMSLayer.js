/**
 * @author johnhollister
 * 
 * @augments	JMap.web.mapLayer.Layer
 * @constructor
 */
JMap.web.mapLayer.MVWMSLayer = function(params) {
	//call base class
	JMap.web.mapLayer.Layer.call(this,params);
	this.serviceType = 'wms';
}


//inherit prototype from Layer
JMap.web.mapLayer.MVWMSLayer.prototype = new JMap.web.mapLayer.Layer();


//redefine getSource method
JMap.web.mapLayer.MVWMSLayer.prototype.getSourceURL = function(x, y) {
	var src = this.baseUrl;
	if (src.indexOf('?') == -1) {
		src += '?';
	} else if (src.indexOf('?') != src.length - 1) {
		src += '&';
	}
	
	var degreesPerPxX = (360 / this.map.tileSize) / this.map.projection.getTilesPerMapX();
	var degreesPerPxY = (180 / this.map.tileSize) / this.map.projection.getTilesPerMapY();
	
	var xmin = ((x * (degreesPerPxX * this.map.tileSize)) % 360) - 180;
	if (180 - xmin  < 0.0000001) xmin = -180;
	var xmax = xmin + (degreesPerPxX * this.map.tileSize);
	var ymin = ((y * (degreesPerPxY * this.map.tileSize))) - 90;
	var ymax = ymin + (degreesPerPxY * this.map.tileSize);  

	//for overlapping tiles
	xmin -= (this.overlapX * degreesPerPxX);
	xmax += (this.overlapX * degreesPerPxX);
	ymin -= (this.overlapY * degreesPerPxY);
	ymax += (this.overlapY * degreesPerPxY);

	var src = 'request=GetMap';
	src += '&datasource=' + this.datasource;
	src += '&srs=EPSG:' + this.srs;
	src += '&version=' + this.version;
	src += '&layers=' + this.layersUrlParam;
	src += "&BBOX=" + xmin + "," + ymin + "," + xmax + "," + ymax;
	src += "&width=" + (this.map.tileSize - (this.overlapX * -2));
	src += "&height=" + (this.map.tileSize - (this.overlapY * -2));           
	src += '&transparent=true';
	src += '&format=' + this.format;   
	src += '&styles='; //according to the spec, styles is *required* in WMS, only support default here 

	return src;
}