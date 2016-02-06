
//Module dependencies
var express  = require('express');
var bodyParser = require('body-parser');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
var fs = require('fs');
var multer = require('multer');
var app      = express();
var path    = require("path");

var Parse = require('parse/node').Parse;

//Parse.initialize("Your App Id", "Your JavaScript Key");
Parse.initialize("jSVbre0kUwZsqd0QBwlrvRuGPjVT4Vqi7n2y91EU", "s1D3awAUsjq9vZsFDbwuPIeMDXiKvOdjXnohvmm5");

var fileUpload_done = false;
var port     = process.env.PORT || 8080;

//Multer configurations for image upload 
app.use(multer({ dest: './uploads/',
        rename: function (fieldname, filename) {
            return filename + "-" + Date.now();
        },
        onFileUploadStart: function (file) {
            //console.log(file.originalname + ' is starting ...');
        },
        onFileUploadComplete: function (file) {
            console.log("******* Image Uploaded to " + file.path + " *****");
            //console.log(file.fieldname + ' uploaded to  ' + file.path);
            fileUpload_done = true;
        }
}));


// Configuration
app.use(express.static(__dirname + '/public'));
app.use(cookieParser());
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended : false }));


//test for connecting to parse
app.get('/parse_find', function(req,res){

    var NutrientInfo = Parse.Object.extend("NutrientInfo");
    var query = new Parse.Query(NutrientInfo);
    query.equalTo("username", "Gohan");
    query.equalTo("productBarcode", "2343434532");

    query.find({
      success: function(results) {
        console.log("Successfully retrieved " + results.length + " objects.");
        // Do something with the returned Parse.Object values
        for (var i = 0; i < results.length; i++) {
          var object = results[i];
          console.log("Username : " + object.get('username'));
        }
      },
      error: function(error) {
        console.log("Error: " + error.code + " " + error.message);
      }
    });

});

app.get('/parse_get', function(req,res){
    var NutrientInfo = Parse.Object.extend("NutrientInfo");
    var query = new Parse.Query(NutrientInfo);
    query.get("BZQFR1RggH", {
      success: function(nutrientInfoObject) {
        // The object was retrieved successfully.
        console.log("Username : " + nutrientInfoObject.get('username'));
      },
      error: function(object, error) {
        // The object was not retrieved successfully.
        // error is a Parse.Error with an error code and message.
      }
    });

});

app.get('/parse_save', function(req,res){
    var NutrientInfo = Parse.Object.extend("NutrientInfo");
    var nutrientInfo = new NutrientInfo();

    nutrientInfo.set("username", "Vegeta");
    nutrientInfo.set("productName", "Nestle Maggi");
    nutrientInfo.set("productBarcode", "9878765456");
    nutrientInfo.set("energy", "500");
    nutrientInfo.set("carbohydrates", "90");
    nutrientInfo.set("sugars", "67");
    nutrientInfo.set("protein", "34");
    nutrientInfo.set("fats", "22");


    nutrientInfo.save(null, {
      success: function(nutrientInfoObject) {
        // Execute any logic that should take place after the object is saved.
        console.log('New object created with objectId: ' + nutrientInfoObject.id);
      },
      error: function(nutrientInfoObject, error) {
        // Execute any logic that should take place if the save fails.
        // error is a Parse.Error with an error code and message.
        console.log('Failed to create new object, with error code: ' + error.message);
      }
    });

});


// Routes
 
app.get('/',function(req,res){
    //res.end("Node-Android");
    res.sendfile('./index.html');
});

//upload for mobile
app.post('/mobileUpload', function(req,res){

    if(fileUpload_done == true){
        console.log("Product Name    : " + req.body.product_name);
        console.log("Product Barcode : " + req.body.product_barcode);
        //console.log(req.body);
        console.log(req.files);
        fileUpload_done = false;
        
        res.setHeader('Content-Type', 'application/json');
        res.send(JSON.stringify({"success" : "Uploaded Successfully", "status" : 200}));
    }
    
});

//upload for pc
app.post('/upload', function(req,res){

    if(fileUpload_done == true){
        console.log(req.files);
        res.end('{"success" : "Uploaded Successfully", "status" : 200}');
        fileUpload_done = false;
    }

});

//upload for pc, run opencv and store in parse
app.post('/upload2', function(req,res){

    if(fileUpload_done == true){

        //console.log("******* Image Uploaded Successfully*****");
        fileUpload_done = false;

        var jsonString;
        //Run the opencv program
        console.log("******* Opencv program called *****");
        const exec = require('child_process').exec;
        const nft = exec('./NFT_new/NFT ./uploads/*.jpg',function(error, stdout, stderr) {

            if (error !== null) {
                console.log("stderr: " + error);
            }    

            if(!stderr){
                
                jsonString = JSON.parse(stdout);
                console.log("******* Retrieved result from OpenCV program *****");
                console.log("******* " + JSON.stringify(jsonString) + " *****");

                var NutrientInfo = Parse.Object.extend("NutrientInfo");
                var nutrientInfo = new NutrientInfo();

                nutrientInfo.set("username", "Vegeta");
                nutrientInfo.set("productName", "Nestle Maggi");
                nutrientInfo.set("productBarcode", "9878765456");
                nutrientInfo.set("energy", jsonString.jsonNutrients.Energy + "");
                nutrientInfo.set("carbohydrates", jsonString.jsonNutrients.Carbohydrates + "");
                nutrientInfo.set("sugars", jsonString.jsonNutrients.Sugars + "");
                nutrientInfo.set("protein", jsonString.jsonNutrients.Protein + "");
                nutrientInfo.set("fats", jsonString.jsonNutrients.Fats + "");


                nutrientInfo.save(null, {
                  success: function(nutrientInfoObject) {
                    // Execute any logic that should take place after the object is saved.
                    console.log("******* Saved the object to Parse with objectId : " + nutrientInfoObject.id + " *****");
                    res.end('{"success" : "All operations performed successfully", "status" : 200}');

                  },
                  error: function(nutrientInfoObject, error) {
                    // Execute any logic that should take place if the save fails.
                    // error is a Parse.Error with an error code and message.
                    console.log('Failed to create new object, with error code: ' + error.message);
                  }
                });


            } else {

                console.log("******* Error from opencv program *****");
                console.log(stderr);
                res.end('{"error" : "Error at opencv server", "status" : 500}');
            }

            //cleaning up the uploads folder
            var uploadPath = "./uploads/"
            fs.readdir(uploadPath, function(err, list_of_files) {
                if (err) throw err;
                list_of_files.map(function (file) {
                    return path.join(uploadPath, file);
                }).forEach(function(filename) {
                    fs.unlink(filename);
                    console.log("******* Cleaned up uploads *****");
                });
            });

        });

    }

});

//test for running opencv program
app.get('/opencv', function(req,res){

    var jsonString;
/*
    const spawn = require('child_process').spawn;
    //const nft = spawn('./NFT_new/NFT',['./uploads/*.jpg']);
    const nft = exec('./NFT_new/NFT ./uploads/*.jpg');

    nft.stdout.on('data', function(data) {
        jsonString = JSON.parse(data);
        console.log(jsonString);
        res.end(data);
    });

    nft.stderr.on('data', function(data) {
      console.log("NFT stderr: " + data);
      res.end(data);
    });

    nft.on('close', function(code) {
      console.log("Opencv process exited with code : " + code);
    });
*/
    const exec = require('child_process').exec;
    const nft = exec('./NFT_new/NFT ./uploads/*.jp',function(error, stdout, stderr) {

        if(!stderr){

            jsonString = JSON.parse(stdout);
            console.log(jsonString);
            res.end(stdout);
        } else {

            console.log(stderr);
            res.end(stderr);
        }

        if (error !== null) {
            console.log("stderr: " + error);
        }
    });


});


//test for running string matching c++ program
app.get('/run_stringMatching', function(req,res){
    var jsonString;
	const exec = require('child_process').exec;
    const child = exec('./StringMatching/main',function(error, stdout, stderr) {
        //console.log( "stdout: " + stdout);
        //console.log("stderr: " + stdout);

        jsonString = JSON.parse(stdout);
        console.log(jsonString);

        res.end(stdout);

        if (error !== null) {
            console.log("stderr: " + stderr);
        }
    });

});


 
app.get('/uploads/:file', function (req, res){
    file = req.params.file;
    var dirname = "C:/Users/deeks/Desktop/Node.js/Node-Android";
    var img = fs.readFileSync(dirname + "/uploads/" + file);
    res.writeHead(200, {'Content-Type': 'image/jpg' });
    res.end(img, 'binary');
 
});
 
app.listen(port);
console.log('Node.js server running on port ' + port);