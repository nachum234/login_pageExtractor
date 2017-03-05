//should according to location
var fs = require('fs');

var Q = require('q');
casper.on('remote.message', function(msg) {
    this.echo('remote message caught: ' + msg);
});

casper.login = function(url,storeFolder,checkUrl) {
	RESULT['loginStatus'] = 500;

	if(valid(url,storeFolder,checkUrl)==1){

		isloggedPromise(checkUrl)
		.then(function (value) {
			// ok
			console.log("we got from promoise", value);
		}, function (reason) {
			// fail
			console.log("we not from promoise", reason);
			console.log(" Need login ");
			casper.thenOpen(url, function() {
				console.log("The login url is "+url);

				fillForm()
					.then(function(){
						checkForm(storeFolder);
					});

			});
		});

	}

};

	function valid(url,storeFolder,checkUrl){
		console.log("validate login params...");
		if(url == null || storeFolder==null || checkUrl==null){
			console.log("invalid login data");
			return 0;
		}
		console.log("valid login data");
		return 1;
	}

	function fillForm(){
   //Now we have to populate username and password, and submit the form
		return Q.Promise(function(resolve) {
			var _this = this;
			casper.then(function(){
				this.evaluate(function(){

					#loginFill#


				});
				setTimeout(resolve.bind(_this), 1000);
			});
		});
	};



	//Wait to be redirected to the Home page, and then make a screenshot
	function checkForm(storeFolder){
			casper.then(function(){
			var fl = storeFolder+"/AfterLogin.png";
			this.capture(fl);
			console.log("create AfterLogin.png" +fl);
			
			var isFound = this.evaluate(function(){					
					#loginValidation# 
				});
			
			
			console.log(" found ? " + isFound);
			
			if(isFound ){
				RESULT['loginStatus'] = 200;
				this.echo("The login passed.Redirected to page with title "+this.getTitle()+" welcome msg ");			
				
			}
			else{
				RESULT['loginStatus'] = 500;
			}
				
					
		});
	};
		
	function isloggedPromise(url) {
		 return Q.Promise(function(resolve, reject) {
			 casper.thenOpen(url,function(){	
				var isFound = this.evaluate(function(){					
					 #loginValidation# 
				});
			
		
				console.log("logged str found ? " + isFound);	
				if(isFound ){
					RESULT['loginStatus'] = 200;
					resolve(200);
				}
				else{
					RESULT['loginStatus'] = 500;
					reject(500);
				}				
			});
		 });
	}
	
