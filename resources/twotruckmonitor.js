'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('towtruck', ['ui.bootstrap','ngSanitize']);






// --------------------------------------------------------------------------
//
// Controler TowTruck
//
// --------------------------------------------------------------------------


appCommand.controller('TowTruckControler',
	function ( $http, $scope, $sce ) {
	this.isshowhistory=false;
	this.pingdate='';
	this.pinginfo='';
	this.inprogress=false;

	
	this.showhistory = function( show )
	{
	   this.isshowhistory = show;
	}

	this.navbaractiv='environment';
	
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
		 return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}
	// ------------------------------------------------------------------------------
	//    Timer
	// ------------------------------------------------------------------------------
	this.createmissingtimers = function( typeCreation)
	{

			
		var self=this;
		self.inprogress=true;
		
		$http.get( '?page=custompage_towtruck&action=createmissingtimers&typecreation='+typeCreation+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.listtimers 		= jsonResult.listtimers;
						self.missingtimerstatus	= jsonResult.missingtimerstatus;
						self.timererror 		= jsonResult.timererror;
						self.inprogress			= false;
					})
				.error( function() {
					
						self.timerstatus 		= jsonResult.timerstatus;
						self.timererror 		= jsonResult.timererror;
						self.inprogress			= false;
					});
				
	}; 
	
	this.getmissingtimer = function()
	{
		
		var self=this;
		self.inprogress=true;

		$http.get( '?page=custompage_towtruck&action=getmissingtimer'+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.listtimers 		= jsonResult.listtimers;
						self.timerstatus 		= jsonResult.timerstatus;
						self.timererror 		= jsonResult.timererror;
								
						self.inprogress=false;

				})
				.error( function() {
					alert('an error occured');
						self.timererror 		= jsonResult.timererror;
						self.inprogress=false;
					});
				
	}; // end getmissingtimer

	
	this.deletetimers = function()
	{
		
		var self=this;
		self.inprogress=true;

		$http.get( '?page=custompage_towtruck&action=deletetimers'+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.listtimers 		= jsonResult.listtimers;
						self.timerstatus 		= jsonResult.timerstatus;
						self.timererror 		= jsonResult.timererror;
								
						self.inprogress=false;
				})
				.error( function() {

						self.timerstatus 		= jsonResult.timerstatus;
						self.timererror 		= jsonResult.timererror;
						self.inprogress=false;
					});
				
	}; // end deletetimer

	
	// ------------------------------------------------------------------------------
	//    Groovy
	// ------------------------------------------------------------------------------

	this.groovy = { "type": '', "code":"", "src": 'return "Hello Word";' };
	this.listUrlCall=[];
	this.groovyload = function() 
	{
		var self=this;
		self.inprogress	=true;
		self.groovy.result="";
		self.groovy.type			= 'code';
		self.groovy.listevents=""
		self.groovy.result="";
		self.groovy.exception="";
		
		$http.get( '?page=custompage_towtruck&action=groovyload&code='+ this.groovy.code+'&t='+Date.now() )
		.success( function ( jsonResult ) {
				console.log("history",jsonResult);
				self.groovy.loadstatus 	= "Script loaded";
				
				self.groovy.parameters 		= jsonResult.placeholder;
				self.groovy.listeventsload	= jsonResult.listevents;
				self.groovy.directRestApi	= jsonResult.directRestApi;
				self.groovy.groovyResolved  = jsonResult.groovyResolved;
				self.groovy.title			= jsonResult.title;
				self.groovy.description		= jsonResult.description;
				
				self.inprogress				= false;
		})
		.error( function() {
				self.groovy.loadstatus="Error at loading."
				self.inprogress=false;
			});
	}
	
	this.groovyexecute = function()
	{
		var self=this;
		self.inprogress=true;
		self.groovy.result="";
		self.groovy.listeventsload="";
		self.groovy.listevents='';
		self.groovy.result="";
		self.groovy.exception="";
		
		var param = {'type': self.groovy.type};
		
		if (self.groovy.type=='code')
		{
			param.placeholder = self.groovy.parameters;
			param.code = self.groovy.code;
		}
		else
			param.src = self.groovy.src;
		
		// groovy page does not manage the POST, and the groovy may be very big : so, let's trunk it
		this.listUrlCall=[];
		// this.listUrlCall.push( "action=collect_reset");
		
		// prepare the string

		var json = angular.toJson( param, false);

		// split the string by packet of 5000 
		while (json.length>0)
		{
			var jsonFirst = encodeURIComponent( json.substring(0,5000));
			json =json.substring(5000);
			var action="";
			if (json.length==0)
				action="groovyexecute";
			else
				action="collect_add";
			this.listUrlCall.push( "action="+action+"&paramjson="+jsonFirst);

		}
		var self=this;
		// self.listUrlCall.push( "action=groovyexecute");
		
		
		self.listUrlIndex=0;
		self.executeListUrl( self ) // , self.listUrlCall, self.listUrlIndex );
		
	
	}
	
	
	// ------------------------------------------------------------------------------------------------------
	// List Execution
	
	this.executeListUrl = function( self ) // , listUrlCall, listUrlIndex )
	{
		console.log(" Call "+self.listUrlIndex+" : "+self.listUrlCall[ self.listUrlIndex ]);
		self.listUrlPercent= Math.round( (100 *  self.listUrlIndex) / self.listUrlCall.length);
		
		$http.get( '?page=custompage_towtruck&'+self.listUrlCall[ self.listUrlIndex ]+'&t='+Date.now() )
			.success( function ( jsonResult ) {
				// console.log("Correct, advance one more",
				// angular.toJson(jsonResult));
				self.listUrlIndex = self.listUrlIndex+1;
				if (self.listUrlIndex  < self.listUrlCall.length )
					self.executeListUrl( self ) // , self.listUrlCall,
												// self.listUrlIndex);
				else
				{
					console.log("Finish", angular.toJson(jsonResult));
					self.inprogress=false;
					self.listUrlPercent= 100; 
					self.groovy.result			= jsonResult.result;
					self.groovy.listevents		= jsonResult.listevents;
					
					self.groovy.exception   	= jsonResult.exception;
					self.groovy.directRestApi	= jsonResult.directRestApi;
					self.groovy.groovyResolved  = jsonResult.groovyResolved;

				}
			})
			.error( function() {
				self.inprogress=false;

				// alert('an error occure');
				});	
		};
	
		
		this.getListEvents = function ( listevents ) {
			return $sce.trustAsHtml(  listevents );
		}

});



})();