
//Module dependencies
var express  = require('express');
var bodyParser = require('body-parser');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
var fs = require('fs');
var multerNft = require('multer');
var multerProduct = require('multer');
var app      = express();
var path    = require("path");

var Parse = require('parse/node').Parse;

//Parse.initialize("Your App Id", "Your JavaScript Key");
Parse.initialize("jSVbre0kUwZsqd0QBwlrvRuGPjVT4Vqi7n2y91EU", "s1D3awAUsjq9vZsFDbwuPIeMDXiKvOdjXnohvmm5");

var nftFileUpload_done = false;
var productFileUpload_done = false;

var tempProductPicUploadsfilePath;
var tempProductPicUploadsfilename;

var port     = process.env.PORT || 8080;

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

    if(nftFileUpload_done == true){
        console.log("Product Name    : " + req.body.product_name);
        console.log("Product Barcode : " + req.body.product_barcode);
        //console.log(req.body);
        console.log(req.files);
        nftFileUpload_done = false;
        
        res.setHeader('Content-Type', 'application/json');
        res.send(JSON.stringify({"success" : "Uploaded Successfully", "status" : 200}));
    }
    
});

//product pic upload for pc, run opencv feature matching and return
// whether similar image is found or not
app.post('/uploadProductPic', multerProduct({ 
        dest: './tempProductPicUploads/',
        changeDest: function(dest, req, res) {
            //var newDestination = dest + req.params.type;
            var newDestination = dest + 'n3hlRyuPko';
            var stat = null;
            try {
                stat = fs.statSync(newDestination);
            } catch (err) {
                fs.mkdirSync(newDestination);
            }
            if (stat && !stat.isDirectory()) {
                throw new Error('Directory cannot be created because an inode of a different type exists at "' + dest + '"');
            }
            return newDestination
        },
        rename: function (fieldname, filename) {
            tempProductPicUploadsfilename = filename + "-" + Date.now();
            return tempProductPicUploadsfilename;
        },
        onFileUploadStart: function (file) {
        },
        onFileUploadComplete: function (file) {
            tempProductPicUploadsfilePath = file.path + "";
            console.log("******* Image Uploaded to " + file.path + " *****");
            productFileUpload_done = true;
        }
    }), function(req,res){

    if(productFileUpload_done == true){

        productFileUpload_done = false;

        var jsonString;

        var argumentString = [];

        argumentString.push(tempProductPicUploadsfilePath);

        //userId will be got from req.body
        var userId = 'n3hlRyuPkoz';

        var uploadPath = './productPicUploads/' + userId + '/';

        if (fs.existsSync(uploadPath)) {
            var list_of_files = fs.readdirSync(uploadPath);
            for (var file in list_of_files) {
                //console.log("filename : " + path.join(uploadPath, list_of_files[file]));
                argumentString.push(path.join(uploadPath, list_of_files[file]));
            }

        } else{
            console.log('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
            res.end('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
        }

        //console.log("argumentString : " + argumentString);

        if(argumentString.length < 2 ) {

            console.log('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
            res.end('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');

        } else {

            //Run the opencv feature matching program
            console.log("******* Opencv Feature Matching program called *****");
            const execFile = require('child_process').execFile;
            const nft = execFile('./FeatureMatching_new/FeatureMatching', argumentString,function(error, stdout, stderr) {

                if (error !== null) {
                    console.log("stderr: " + error);
                }    

                if(!stderr){

                    jsonString = JSON.parse(stdout);
                    console.log("******* Retrieved result from OpenCV Feature matching program *****");
                    console.log("******* " + JSON.stringify(jsonString) + " *****");

                    if(jsonString.isFoundSimilar == 1){


                        //find the barcode for the product pic with the userid and productPicLocation
                        var ProductImage = Parse.Object.extend("ProductImage");
                        var query = new Parse.Query(ProductImage);
                        query.equalTo("userId", userId);
                        query.equalTo("productPicLocation", "./" + jsonString.imageName);

                        query.find({
                          success: function(results) {
                            console.log("Successfully retrieved " + results.length + " objects.");

                            if(results.length){

                                var object = results[0];
                                console.log("Barcode : " + object.get('barcode'));

                                //cleaning up the temp product uploads folder for tht user
                                var uploadPath = './tempProductPicUploads/' + userId + '/';
                                fs.readdir(uploadPath, function(err, list_of_files) {
                                    if (err) throw err;
                                    list_of_files.map(function (file) {
                                        return path.join(uploadPath, file);
                                    }).forEach(function(filename) {
                                        fs.unlink(filename);
                                        console.log("******* Cleaned up temp product uploads *****");
                                    });
                                });
                                console.log('{"found" : true, barcode :\"' + object.get('barcode') + '\", "status" : 200}');
                                res.end('{"found" : true, barcode :\"' + object.get('barcode') + '\", "status" : 200}');
                            }
                            else{
                                console.log('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                                res.end('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                            }
                          },
                          error: function(error) {
                            console.log("Error: " + error.code + " " + error.message);
                          }
                        });

                    } else {
                        console.log('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\" "status" : 200}');
                        res.end('{"found" : "false", "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\" "status" : 200}');
                            
                    }

                } else {

                    console.log("******* Error from opencv feature matching program *****");
                    console.log(stderr);
                    res.end('{"error" : "Error at opencv server", "status" : 500}');
                }

                //res.end('{"success" : "All operations performed successfully", "status" : 200}');

            });

        }

    }

});

//nft pic upload for pc, run opencv and return json nutrients to android
app.post('/uploadNFTPic', multerNft({ 
        dest: './uploads/',
        rename: function (fieldname, filename) {
            return filename + "-" + Date.now();
        },
        onFileUploadStart: function (file) {
            //console.log(file.originalname + ' is starting ...');
        },
        onFileUploadComplete: function (file) {
            console.log("******* Image Uploaded to " + file.path + " *****");
            //console.log(file.fieldname + ' uploaded to  ' + file.path);
            nftFileUpload_done = true;
        }
    }),function(req,res){

    if(nftFileUpload_done == true){

        //console.log("******* Image Uploaded Successfully*****");
        nftFileUpload_done = false;

        //retrieved from req.body
        var userEmail = 'ash@gmail.com';
        var userId = 'n3hlRyuPko';
        var barcode = '9878765456';
        var tempProductPicUploadsfilename = 'paper2-1453651369838.jpg';
        var tempProductPicUploadsfilePath = './tempProductPicUploads/' + userId + '/' + tempProductPicUploadsfilename;

        //move file from tempProductPicUploadsfilePath to userProductPicUploadsfilePath
        var userProductPicUploadsfilePath = './productPicUploads/' + userId + '/' + tempProductPicUploadsfilename;
        console.log("tempProductPicUploadsfilePath : " + tempProductPicUploadsfilePath);
        console.log("userProductPicUploadsfilePath : " + userProductPicUploadsfilePath);
        fs.rename(tempProductPicUploadsfilePath, userProductPicUploadsfilePath, function(err){
            if (err) res.json(err);

            console.log('Image is moved ...');

            //store the userId,barcode,imagepath in
            var ProductImage = Parse.Object.extend("ProductImage");
            var productImage = new ProductImage();


            productImage.set("userId", userId);
            productImage.set("userEmail", userEmail);
            productImage.set("barcode", barcode);
            productImage.set("productPicLocation", userProductPicUploadsfilePath);


            productImage.save(null, {
              success: function(productImageObject) {

                console.log("******* Saved the object to Parse with objectId : " + productImageObject.id + " *****");
                
                var jsonString;
                //Run the opencv program
                console.log("******* Opencv NFT program called *****");
                const exec = require('child_process').exec;
                const nft = exec('./NFT_new/NFT ./uploads/*.jpg',function(error, stdout, stderr) {

                    if (error !== null) {
                        console.log("stderr: " + error);
                    }    

                    if(!stderr){
                        
                        jsonString = JSON.parse(stdout);
                        console.log("******* Retrieved result from OpenCV program *****");
                        console.log("******* " + JSON.stringify(jsonString) + " *****");


                        //return the json back to android
                        console.log('{"success" : "All operations performed successfully", "status" : 200, "jsonNutrients" : '+ JSON.stringify(jsonString.jsonNutrients) +'}');
                        res.end('{"success" : "All operations performed successfully", "status" : 200, "jsonNutrients" : '+ JSON.stringify(jsonString.jsonNutrients) +'}');

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

              },
              error: function(productImageObject, error) {

                console.log('Failed to create new object, with error code: ' + error.message);
              }
            });

        });
    }

});

//nutrient info upload  for pc, save it in parse
app.post('/uploadNutrientInfo', function(req,res){

    //retrieved from request
    var username = "ash@gmail.com" ;
    var productName = "Kissan Jam";
    var productBarcode = "9878765456";
    var energy = "500";
    var carbohydrates = "70"; 
    var sugars = "24";
    var protein = "7";
    var fats = "20";



    var NutrientInfo = Parse.Object.extend("NutrientInfo");
    var nutrientInfo = new NutrientInfo();

    nutrientInfo.set("username", username);
    nutrientInfo.set("productName", productName);
    nutrientInfo.set("productBarcode", productBarcode);
    nutrientInfo.set("energy", energy);
    nutrientInfo.set("carbohydrates", carbohydrates);
    nutrientInfo.set("sugars", sugars);
    nutrientInfo.set("protein", protein);
    nutrientInfo.set("fats", fats);


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
        res.end('{"error" : "Error during saving to parse", "status" : 500}');
      }
    });

});

//upload for pc, run opencv and store in parse
app.post('/upload2', multerNft({ 
        dest: './uploads/',
        rename: function (fieldname, filename) {
            return filename + "-" + Date.now();
        },
        onFileUploadStart: function (file) {
            //console.log(file.originalname + ' is starting ...');
        },
        onFileUploadComplete: function (file) {
            console.log("******* Image Uploaded to " + file.path + " *****");
            //console.log(file.fieldname + ' uploaded to  ' + file.path);
            nftFileUpload_done = true;
        }
}),function(req,res){

    if(nftFileUpload_done == true){

        //console.log("******* Image Uploaded Successfully*****");
        nftFileUpload_done = false;

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

 
app.listen(port);
console.log('Node.js server running on port ' + port);