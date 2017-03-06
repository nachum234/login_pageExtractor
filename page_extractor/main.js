debugger;
var fs = require('fs');

//casperjs --verbose --cookies-file=cookies.txt  main.js --loginUrl="https://login.luminate.com/login" --scanUrl="https://www.aabacosmallbusiness.com/" --loginScript="C:/opt/bis/casper_scripts/login.js" --user="anna.kuranda@biscience.com" --password="Abcdefganna1!"  --storeUrlFile="C:/opt/bis/casper_scripts/jobid/output/urlsRunid.txt" --storeFolder="C:/opt/bis/casper_scripts/output/" --loginValidation="anna-anna.net"  
//casperjs --verbose --cookies-file=cookies3.txt  main.js --loginUrl="http://www.healthyplace.com/index.php?option=com_users&view=login" --scanUrl="http://www.healthyplace.com/about-healthyplace/about-us/about-healthyplace/" --loginScript="C:/opt/bis/casper_scripts/login.js" --user="annatest" --password="Abcdefganna1!"  --storeUrlFile="C:/opt/bis/casper_scripts/jobid/output/urlsRunid.txt" --storeFolder="C:/opt/bis/casper_scripts/output1/" --loginValidation="a[href*='logout']" --checkRedirect="true"



var urlList = [];
var RESULT = {};
RESULT['loginStatus']=500;

var loginUrl;
var scanUrl;
var loginScript;
var storeUrlFile;
var storeFolder;
var checkRedirect;



//dont touch the string.used by java
var statusMsg = "The login done with status ";



var casper = require('casper').create({
	 verbose: true, 
     logLevel: 'error',
	
	viewportSize: {
        width: 1024,
        height: 1000
    },
    pageSettings: {
        loadImages: true,//The script is much faster when this field is set to false
        loadPlugins: true,
        userAgent: 'Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.117 Safari/537.36'
		
    }	
});


casper.echo("Casper CLI passed options:");
require("utils").dump(casper.cli.options);


//init
loginScript =casper.cli.get("loginScript");
loginUrl =casper.cli.get("loginUrl");
scanUrl =casper.cli.get("scanUrl");



//save found urls
storeUrlFile =casper.cli.get("storeUrlFile");
//store run time created png or other files
storeFolder =casper.cli.get("storeFolder");
checkRedirect =casper.cli.get("checkRedirect");



casper.on('remote.message', function(msg) {
    this.echo('remote message caught: ' + msg);
});


casper.on("page.error", function(msg, trace) {
    this.echo("Page Error: " + msg, "ERROR");
});


//validate input
if(loginUrl==="" || loginUrl ==null || scanUrl== null||scanUrl==="" || loginScript==="" || loginScript==null  || storeUrlFile==="" || storeUrlFile==null || storeFolder==="" || storeFolder==null  ){
	console.log("One of passed parameters empty.Should not run fetch pages in casperjs ");
	var logmsg = statusMsg+RESULT['loginStatus'];
	console.log(logmsg);
	console.log("Finished");
	casper.exit();
}

if(checkRedirect==="" || checkRedirect ==null){
	checkRedirect=false;
	
}

console.log("Check redirect "+checkRedirect);

phantom.injectJs(loginScript);
   

casper.start().then(function() {	
console.log("in main before");
    //temporary removed.Failed to run on linux
	//casper.login(loginUrl,storeFolder,scanUrl);
	RESULT['loginStatus']=200;
});




casper.then(function() {	
	var logmsg = statusMsg+RESULT['loginStatus'];
	console.log(logmsg);
	if(RESULT['loginStatus'] == 500){
		console.log("Finished 500");
		casper.exit();
	}
});


//Second step is open page after login
casper.thenOpen(scanUrl, function() {	
	if(RESULT['loginStatus'] == 200){
		console.log("Scan page "+scanUrl);
		var op = this.getCurrentUrl();									
		this.echo("opened " + op);
		if(checkRedirect==true){
			try {					
					fs.write(storeUrlFile,  op+"\n", 'a');
				} catch(err) {
					this.log(f("Failed to save page html to %s; please check permissions ",storeUrlFile), "error");
					this.log(err, "debug");
					
				}
				console.log("Finished for check redirect flow");
				this.exit();
				
				
		}
		
		this.echo("Get links method 1")
		var links = this.evaluate(function(){
		var links = document.getElementsByTagName('a');
			links = Array.prototype.map.call(links,function(link){
				return link.getAttribute('href');
			});
			return links;	
		});
		
		

		
		//var fs = require('fs');	
		this.echo("print links mehod 1");			
		this.each(links,function(self,link){
			self.thenOpen(link,function(a){	
				var r = this.getCurrentUrl();			
				urlList.push(r);							
				this.echo(r);	
				try {
					
					fs.write(storeUrlFile,  this.getCurrentUrl()+"\n", 'a');
				} catch(err) {
					this.log(f("Failed to save page html to %s; please check permissions ",storeUrlFile), "error");
					this.log(err, "debug");
					
				}
				
			});
		});	
		
	
	}	
	else{
		console.log("Scan page should be not performed ");
	}
    
});












casper.run(function(){

	console.log("Finished");
	this.exit();
	
	
});

