
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

//for running web ocr
var querystring = require('querystring');
var http = require('http');

var Parse = require('parse/node').Parse;

//Parse.initialize("Your App Id", "Your JavaScript Key");
Parse.initialize("jSVbre0kUwZsqd0QBwlrvRuGPjVT4Vqi7n2y91EU", "s1D3awAUsjq9vZsFDbwuPIeMDXiKvOdjXnohvmm5");

//www.ocrwebservice.com authenication
var ocr_web_service_license_code = "8DDC90BE-9E9B-4416-A8DE-75B7E323899E";
var ocr_web_service_user_name =  "ABHIRAM";

var nftFileUpload_done = false;
var productFileUpload_done = false;

var tempProductPicUploadsfilePath;
var tempProductPicUploadsfilename;
var userIdFileUpload;

//for nftPicUpload2
var nftFilename;

var port     = process.env.PORT || 8080;

// Configuration
app.use(express.static(__dirname + '/public'));
app.use(cookieParser());
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended : false }));


// Routes
 
app.get('/',function(req,res){
    //res.end("Node-Android");

    res.sendfile('./index.html');
});

//product pic upload for mobile, run opencv feature matching and return
// whether similar image is found or not
app.post('/uploadProductPicMobile', multerProduct({ 
        dest: './uploads/temp_products/',
        changeDest: function(dest, req, res) {
            //var newDestination = dest + req.params.type;
            userIdFileUpload = req.params.user_id;
            console.log("req.params : " + JSON.stringify(req.params));
            console.log("req.body : " + JSON.stringify(req.body));
            //userIdFileUpload = userIdFileUpload.replace(/["']/g,"");
            //var newDestination = dest + userIdFileUpload;
            var newDestination = dest;
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
        var userId = req.body.user_id;
        userId = userId.replace(/["']/g,"");
        console.log("userId : " + userId)

        var uploadPath = './uploads/user_products/' + userId + '/';

        res.setHeader('Content-Type', 'application/json');

        if (fs.existsSync(uploadPath)) {
            var list_of_files = fs.readdirSync(uploadPath);
            for (var file in list_of_files) {
                //console.log("filename : " + path.join(uploadPath, list_of_files[file]));
                argumentString.push(path.join(uploadPath, list_of_files[file]));
            }

        } else{
            console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
            res.send(JSON.stringify({"found" : false, "tempProductPicUploadsfilename" : tempProductPicUploadsfilename + ".jpg", "status" : 200}));
        }

        //console.log("argumentString : " + argumentString);

        if(argumentString.length < 2 ) {

            console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
            res.send(JSON.stringify({"found" : false, "tempProductPicUploadsfilename" : tempProductPicUploadsfilename + ".jpg", "status" : 200}));

        } else {

            //Run the opencv feature matching program
            console.log("******* Opencv Feature Matching program called *****");
            const execFile = require('child_process').execFile;
            const nft = execFile('./opencv/FeatureMatching/FeatureMatching', argumentString,function(error, stdout, stderr) {

                if (error !== null) {
                    console.log("stderr: " + error);
                    res.send(JSON.stringify({"error" : "stderr : " + error, "status" : 500}));
                }    

                if(!stderr){

                    jsonString = JSON.parse(stdout);
                    console.log("******* Retrieved result from OpenCV Feature matching program *****");
                    console.log("******* " + JSON.stringify(jsonString) + " *****");

                    if(jsonString.isFoundSimilar == 1){

                        console.log("jsonString.isFoundSimilar : " + jsonString.isFoundSimilar);


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
                                //var uploadPath = './uploads/temp_products/' + userId + '/';
                                var uploadPath = './uploads/temp_products/' + tempProductPicUploadsfilename + '.jpg';
                                fs.unlinkSync(uploadPath);

                                console.log('{"found" : true, "barcode" :\"' + object.get('barcode') + '\", "status" : 200}');
                                res.send(JSON.stringify({"found" : true, "barcode" :  object.get('barcode') , "status" : 200}));
                            }
                            else{
                                console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                                res.send(JSON.stringify({"found" : false, "tempProductPicUploadsfilename" : tempProductPicUploadsfilename + ".jpg", "status" : 200}));
                            }
                          },
                          error: function(error) {
                            console.log("Error: " + error.code + " " + error.message);
                            res.send(JSON.stringify({"error" : error.code + " " + error.message, "status" : 500}));
                          }
                        });

                    } else {
                        console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                        res.send(JSON.stringify({"found" : false, "tempProductPicUploadsfilename" : tempProductPicUploadsfilename + ".jpg", "status" : 200}));
                            
                    }

                } else {

                    console.log("******* Error from opencv feature matching program *****");
                    console.log(stderr);
                    res.send(JSON.stringify({"error" : "Error at opencv server", "status" : 500}));
                }

                //res.end('{"success" : "All operations performed successfully", "status" : 200}');

            });

        }

    }

});

//nft pic upload for mobile, run opencv to crop the image , run ocr webservice  and return json nutrients to android
app.post('/uploadNFTPicMobile', multerNft({ 
        dest: './uploads/nft_images/',
        rename: function (fieldname, filename) {
            nftFilename = filename + "-" + Date.now()
            return nftFilename;
        },
        onFileUploadStart: function (file) {
            //console.log(file.originalname + ' is starting ...');
        },
        onFileUploadComplete: function (file) {
            console.log("******* Image Uploaded to " + file.path + " *****");
            //console.log(file.fieldname + ' uploaded to  ' + file.path);
            nftFileUpload_done = true;
        }
    }),function(req,response){

    if(nftFileUpload_done == true){

        //console.log("******* Image Uploaded Successfully*****");
        nftFileUpload_done = false;

        response.setHeader('Content-Type', 'application/json');

        //retrieved from req.body
        var userEmail = req.body.user_email;
        userEmail = userEmail.replace(/["']/g,"");
        var userId = req.body.user_id;
        userId = userId.replace(/["']/g,"");
        var barcode = req.body.product_barcode;
        barcode = barcode.replace(/["']/g,"");
        var tempProductPicUploadsfilename = req.body.product_pic_filename;
        tempProductPicUploadsfilename = tempProductPicUploadsfilename.replace(/["']/g,"");
        tempProductPicUploadsfilename = tempProductPicUploadsfilename.replace(/[\\]/g,"");
        //var tempProductPicUploadsfilePath = "./tempProductPicUploads/" + userId + "/" + tempProductPicUploadsfilename;
        var tempProductPicUploadsfilePath = "./uploads/temp_products/" + tempProductPicUploadsfilename;

        //move file from tempProductPicUploadsfilePath to userProductPicUploadsfilePath
        var userProductPicUploadsfilePath = "./uploads/user_products/" + userId + "/" + tempProductPicUploadsfilename;
        console.log("tempProductPicUploadsfilePath : " + tempProductPicUploadsfilePath);
        console.log("userProductPicUploadsfilePath : " + userProductPicUploadsfilePath);

        if (!fs.existsSync("./uploads/user_products/" + userId + "/")) {
            fs.mkdirSync("./uploads/user_products/" + userId + "/");
        }
        fs.rename(tempProductPicUploadsfilePath, userProductPicUploadsfilePath, function(err){
            if (err){
                response.send(JSON.stringify({"error" : "stderr : " + err, "status" : 500}));
                console.log(JSON.stringify({"error" : "stderr : " + err, "status" : 500}));
            }

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
                
                var nutrientInfoString;
                var jsonString;

                //Run the opencv program
                console.log("******* Opencv NFT program called *****");
                var argumentString = [];

                argumentString.push('./uploads/nft_images/' + nftFilename + '.jpg');

                console.log("******* String Matching program called *****");

                const execFile = require('child_process').execFile;
                const nft = execFile('./opencv/NFT_WebOCR/NFT/NFT', argumentString,function(error, stdout, stderr) {
                    if (error !== null) {
                        console.log("stderr: " + error);
                    }    

                    if(!stderr){
                        
                        console.log("******* Opencv cropped the image to table *****");
                        console.log(stdout);
                        //run the webocr here
                        var ocrURL = "www.ocrwebservice.com";
                        var ocrServicePath = "/restservices/processDocument?gettext=true";
                        var croppedNFTImagePath = './uploads/nft_images/' + nftFilename + '.jpg';

                        var options = {
                            host: ocrURL,
                            port: 80,
                            path: ocrServicePath,
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                                'Authorization': 'Basic ' + new Buffer(ocr_web_service_user_name + ':' + ocr_web_service_license_code).toString('base64')
                            }
                        };

                        var postReq = http.request(options, function(res) {
                            console.log('STATUS: ' + res.statusCode);
                            //console.log('HEADERS: ' + JSON.stringify(res.headers));
                            res.setEncoding('utf8');

                            var str = '';

                            res.on('data', function (chunk) {
                                str += chunk;
                            });

                            res.on('end', function () {

                                console.log("******* Retrieved result from ocrwebservice.com *****");

                                if(res.statusCode == 200){

                                    var jsonStr =  JSON.parse(str);
                                    nutrientInfoString = JSON.stringify(jsonStr.OCRText[0][0]);

                                    console.log('Nutrient Info: ' + nutrientInfoString);

                                    var argumentString = [];

                                    argumentString.push(nutrientInfoString);

                                    console.log("******* String Matching program called *****");

                                    const execFile = require('child_process').execFile;
                                    const stringMatching = execFile('./opencv/NFT_WebOCR/StringMatching/StringMatching', argumentString,function(error2, stdout2, stderr2) {

                                        if (error2 !== null) {
                                            console.log("stderr: " + error2);
                                        }    

                                        if(!stderr2){
                                            
                                            jsonString = JSON.parse(stdout2);
                                            console.log("******* Retrieved result from String Matching program *****");
                                            console.log("******* " + JSON.stringify(jsonString) + " *****");

                                            //return the json back to android
                                            console.log('{"success" : "All operations performed successfully", "status" : 200, "jsonNutrients" : '+ JSON.stringify(jsonString.jsonNutrients) +'}');
                                            response.send(JSON.stringify({"success" : "All operations performed successfully", "status" : 200, "energy" : jsonString.jsonNutrients.Energy, "carbohydrates" : jsonString.jsonNutrients.Carbohydrates, "sugars" : jsonString.jsonNutrients.Sugars, "protein" : jsonString.jsonNutrients.Protein, "fats" : jsonString.jsonNutrients.Fats}));

                                        } else {

                                            console.log("******* Error from String Matching program *****");
                                            console.log(stderr2);
                                            response.end('{"error" : "Error at String Matching server", "status" : 500}');
                                        }
                                    });

                                    //cleaning up the uploads folder
                                    var uploadPath = "./uploads/nft_images/";
                                    fs.readdir(uploadPath, function(err, list_of_files) {
                                        if (err) throw err;
                                        list_of_files.map(function (file) {
                                            return path.join(uploadPath, file);
                                        }).forEach(function(filename) {
                                            fs.unlink(filename);
                                            console.log("******* Cleaned up /uploads/nft_images/ *****");
                                        });
                                    });
                                }

                            });
                        });

                        var stream = fs.createReadStream(croppedNFTImagePath);
                        stream.on('data', function(data) {
                            postReq.write(data);
                        });
                        stream.on('end', function() {
                            postReq.end();
                        });

                    } else {

                        console.log("******* Error from opencv program *****");
                        console.log(stderr);
                        response.end('{"error" : "Error at opencv server", "status" : 500}');
                    }

                });

              },
              error: function(productImageObject, error) {

                console.log('Failed to create new object, with error code: ' + error.message);
                response.send(JSON.stringify({"error" : "Failed to create new object, with error code: " + error.message, "status" : 500}));
              }
            });

        });
    }

});

//nft pic upload for mobile, run opencv and return json nutrients to android
app.post('/uploadNFTPicMobile2', multerNft({ 
        dest: './uploads/nft_images/',
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

        res.setHeader('Content-Type', 'application/json');

        //retrieved from req.body
        var userEmail = req.body.user_email;
        userEmail = userEmail.replace(/["']/g,"");
        var userId = req.body.user_id;
        userId = userId.replace(/["']/g,"");
        var barcode = req.body.product_barcode;
        barcode = barcode.replace(/["']/g,"");
        var tempProductPicUploadsfilename = req.body.product_pic_filename;
        tempProductPicUploadsfilename = tempProductPicUploadsfilename.replace(/["']/g,"");
        tempProductPicUploadsfilename = tempProductPicUploadsfilename.replace(/[\\]/g,"");
        //var tempProductPicUploadsfilePath = "./tempProductPicUploads/" + userId + "/" + tempProductPicUploadsfilename;
        var tempProductPicUploadsfilePath = "./uploads/temp_products/" + tempProductPicUploadsfilename;

        //move file from tempProductPicUploadsfilePath to userProductPicUploadsfilePath
        var userProductPicUploadsfilePath = "./uploads/user_products/" + userId + "/" + tempProductPicUploadsfilename;
        console.log("tempProductPicUploadsfilePath : " + tempProductPicUploadsfilePath);
        console.log("userProductPicUploadsfilePath : " + userProductPicUploadsfilePath);

        if (!fs.existsSync("./uploads/user_products/" + userId + "/")) {
            fs.mkdirSync("./uploads/user_products/" + userId + "/");
        }
        fs.rename(tempProductPicUploadsfilePath, userProductPicUploadsfilePath, function(err){
            if (err){
                res.send(JSON.stringify({"error" : "stderr : " + err, "status" : 500}));
                console.log(JSON.stringify({"error" : "stderr : " + err, "status" : 500}));
            }

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
                const nft = exec('./opencv/NFT/NFT ./uploads/nft_images/*.jpg',function(error, stdout, stderr) {

                    if (error !== null) {
                        console.log("stderr: " + error);
                        //res.send(JSON.stringify({"error" : "stderr : " + error, "status" : 500}));
                    }    

                    if(!stderr){
                        
                        jsonString = JSON.parse(stdout);
                        console.log("******* Retrieved result from OpenCV program *****");
                        console.log("******* " + JSON.stringify(jsonString) + " *****");


                        //return the json back to android
                        console.log('{"success" : "All operations performed successfully", "status" : 200, "jsonNutrients" : '+ JSON.stringify(jsonString.jsonNutrients) +'}');
                        res.send(JSON.stringify({"success" : "All operations performed successfully", "status" : 200, "energy" : jsonString.jsonNutrients.Energy, "carbohydrates" : jsonString.jsonNutrients.Carbohydrates, "sugars" : jsonString.jsonNutrients.Sugars, "protein" : jsonString.jsonNutrients.Protein, "fats" : jsonString.jsonNutrients.Fats}));

                    } else {

                        console.log("******* Error from opencv program *****");
                        console.log(stderr);
                        res.send(JSON.stringify({"success" : "All operations performed successfully", "status" : 200, "energy" : 0, "carbohydrates" : 0, "sugars" : 0, "protein" : 0, "fats" : 0}));
                     }

                    //cleaning up the uploads folder
                    var uploadPath = "./uploads/nft_images/"
                    fs.readdir(uploadPath, function(err, list_of_files) {
                        if (err) throw err;
                        list_of_files.map(function (file) {
                            return path.join(uploadPath, file);
                        }).forEach(function(filename) {
                            fs.unlink(filename);
                            console.log("******* Cleaned up /uploads/nft_images/ *****");
                        });
                    });

                });

              },
              error: function(productImageObject, error) {

                console.log('Failed to create new object, with error code: ' + error.message);
                res.send(JSON.stringify({"error" : "Failed to create new object, with error code: " + error.message, "status" : 500}));
              }
            });

        });
    }

});

//nutrient info upload  for mobile, save it in parse
app.post('/uploadNutrientInfoMobile', function(req,res){

    //retrieved from request
    var username = req.body.user_email;
    username = username.replace(/["']/g,"");
    var productName = req.body.product_name;
    productName = productName.replace(/["']/g,"");
    var productBarcode = req.body.product_barcode;
    productBarcode = productBarcode.replace(/["']/g,"");
    var energy = req.body.product_energy;
    energy = energy.replace(/["']/g,"");
    var carbohydrates = req.body.product_carbohydrates;
    carbohydrates = carbohydrates.replace(/["']/g,"");
    var sugars = req.body.product_sugars;
    sugars = sugars.replace(/["']/g,"");
    var protein = req.body.product_protein;
    protein = protein.replace(/["']/g,"");
    var fats = req.body.product_fats;
    fats = fats.replace(/["']/g,"");
    var imageURL = req.body.product_imageURL;
    imageURL = imageURL.replace(/["']/g,"");
    var uploadDate = req.body.product_uploadDate;
    uploadDate = uploadDate.replace(/["']/g,"");



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
    nutrientInfo.set("imageURL", imageURL);
    nutrientInfo.set("uploadDate", uploadDate);
    nutrientInfo.set("scansPerDay", 1);

    res.setHeader('Content-Type', 'application/json');

    nutrientInfo.save(null, {
      success: function(nutrientInfoObject) {
        // Execute any logic that should take place after the object is saved.
        console.log("******* Saved the object to Parse with objectId : " + nutrientInfoObject.id + " *****");
        res.send(JSON.stringify({"success" : "All operations performed successfully", "status" : 200}));

      },
      error: function(nutrientInfoObject, error) {
        // Execute any logic that should take place if the save fails.
        // error is a Parse.Error with an error code and message.
        console.log('Failed to create new object, with error code: ' + error.message);
        res.send(JSON.stringify({"error" : "Error during saving to parse", "status" : 500}));
      }
    });

});



//product pic upload for pc, run opencv feature matching and return
// whether similar image is found or not
app.post('/uploadProductPic', multerProduct({ 
        dest: './uploads/temp_products/',
        changeDest: function(dest, req, res) {
            //var newDestination = dest + req.params.type;
            userIdFileUpload = 'n3hlRyuPko'
            var newDestination = dest + userIdFileUpload;
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

            //cleaning up the temp product uploads folder for tht user
            var uploadPath = './uploads/temp_products/' + userIdFileUpload + '/';
            if (fs.existsSync(uploadPath)) {
                var list_of_files = fs.readdirSync(uploadPath);
                list_of_files.map(function (file) {
                    return path.join(uploadPath, file);
                }).forEach(function(filename) {
                    fs.unlink(filename);
                    console.log("******* Cleaned up temp product uploads *****");
                });
            }
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
        var userId = 'n3hlRyuPko';

        var uploadPath = './uploads/user_products/' + userId + '/';

        if (fs.existsSync(uploadPath)) {
            var list_of_files = fs.readdirSync(uploadPath);
            for (var file in list_of_files) {
                //console.log("filename : " + path.join(uploadPath, list_of_files[file]));
                argumentString.push(path.join(uploadPath, list_of_files[file]));
            }

        } else{
            console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
            res.end('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
        }

        //console.log("argumentString : " + argumentString);

        if(argumentString.length < 2 ) {

            console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
            res.end('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');

        } else {

            //Run the opencv feature matching program
            console.log("******* Opencv Feature Matching program called *****");
            const execFile = require('child_process').execFile;
            const nft = execFile('./opencv/FeatureMatching/FeatureMatching', argumentString,function(error, stdout, stderr) {

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
                                var uploadPath = './uploads/temp_products/' + userId + '/';
                                fs.readdir(uploadPath, function(err, list_of_files) {
                                    if (err) throw err;
                                    list_of_files.map(function (file) {
                                        return path.join(uploadPath, file);
                                    }).forEach(function(filename) {
                                        fs.unlink(filename);
                                        console.log("******* Cleaned up temp product uploads *****");
                                    });
                                });
                                console.log('{"found" : true, "barcode" :\"' + object.get('barcode') + '\", "status" : 200}');
                                res.end('{"found" : true, "barcode" :\"' + object.get('barcode') + '\", "status" : 200}');
                            }
                            else{
                                console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                                res.end('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                            }
                          },
                          error: function(error) {
                            console.log("Error: " + error.code + " " + error.message);
                          }
                        });

                    } else {
                        console.log('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                        res.end('{"found" : false, "tempProductPicUploadsfilename" : \"' + tempProductPicUploadsfilename + '.jpg\", "status" : 200}');
                            
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
        dest: './uploads/nft_images/',
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
        var tempProductPicUploadsfilePath = './uploads/temp_products/' + userId + '/' + tempProductPicUploadsfilename;

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
                const nft = exec('./opencv/NFT/NFT ./uploads/nft_images/*.jpg',function(error, stdout, stderr) {

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
                    var uploadPath = "./uploads/nft_images/"
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

//nft pic upload for pc, run opencv to crop the image , run ocr webservice and return json nutrients to android
app.post('/uploadNFTPic2', multerNft({ 
        dest: './uploads/nft_images/',
        rename: function (fieldname, filename) {
            nftFilename = filename + "-" + Date.now()
            return nftFilename;
        },
        onFileUploadStart: function (file) {
        },
        onFileUploadComplete: function (file) {
            console.log("******* Image Uploaded to " + file.path + " *****");
            nftFileUpload_done = true;
        }
    }),function(req,response){

    if(nftFileUpload_done == true){

        //console.log("******* Image Uploaded Successfully*****");
        nftFileUpload_done = false;

        var nutrientInfoString;
        var jsonString;

        //Run the opencv program
        console.log("******* Opencv NFT program called *****");
        var argumentString = [];

        argumentString.push('./uploads/nft_images/' + nftFilename + '.jpg');

        console.log("******* String Matching program called *****");

        const execFile = require('child_process').execFile;
        const nft = execFile('./opencv/NFT_WebOCR/NFT/NFT', argumentString,function(error, stdout, stderr) {
            if (error !== null) {
                console.log("stderr: " + error);
            }    

            if(!stderr){
                
                console.log("******* Opencv cropped the image to table *****");
                console.log(stdout);
                //run the webocr here
                var license_code = "8DDC90BE-9E9B-4416-A8DE-75B7E323899E";
                var user_name =  "ABHIRAM";
                var ocrURL = "www.ocrwebservice.com";
                var ocrServicePath = "/restservices/processDocument?gettext=true";
                var croppedNFTImagePath = './uploads/nft_images/' + nftFilename + '.jpg';

                var options = {
                    host: ocrURL,
                    port: 80,
                    path: ocrServicePath,
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Basic ' + new Buffer(user_name + ':' + license_code).toString('base64')
                    }
                };

                var postReq = http.request(options, function(res) {
                    console.log('STATUS: ' + res.statusCode);
                    //console.log('HEADERS: ' + JSON.stringify(res.headers));
                    res.setEncoding('utf8');

                    var str = '';

                    res.on('data', function (chunk) {
                        str += chunk;
                    });

                    res.on('end', function () {

                        console.log("******* Retrieved result from ocrwebservice.com *****");

                        if(res.statusCode == 200){

                            var jsonStr =  JSON.parse(str);
                            nutrientInfoString = JSON.stringify(jsonStr.OCRText[0][0]);

                            console.log('Nutrient Info: ' + nutrientInfoString);

                            var argumentString = [];

                            argumentString.push(nutrientInfoString);

                            console.log("******* String Matching program called *****");

                            const execFile = require('child_process').execFile;
                            const stringMatching = execFile('./opencv/NFT_WebOCR/StringMatching/StringMatching', argumentString,function(error2, stdout2, stderr2) {

                                if (error2 !== null) {
                                    console.log("stderr: " + error2);
                                }    

                                if(!stderr2){
                                    
                                    jsonString = JSON.parse(stdout2);
                                    console.log("******* Retrieved result from String Matching program *****");
                                    console.log("******* " + JSON.stringify(jsonString) + " *****");

                                    //return the json back to android
                                    console.log('{"success" : "All operations performed successfully", "status" : 200, "jsonNutrients" : '+ JSON.stringify(jsonString.jsonNutrients) +'}');
                                    response.send(JSON.stringify({"success" : "All operations performed successfully", "status" : 200, "energy" : jsonString.jsonNutrients.Energy, "carbohydrates" : jsonString.jsonNutrients.Carbohydrates, "sugars" : jsonString.jsonNutrients.Sugars, "protein" : jsonString.jsonNutrients.Protein, "fats" : jsonString.jsonNutrients.Fats}));

                                } else {

                                    console.log("******* Error from String Matching program *****");
                                    console.log(stderr2);
                                    response.end('{"error" : "Error at String Matching server", "status" : 500}');
                                }
                            });

                            //cleaning up the uploads folder
                            var uploadPath = "./uploads/nft_images/";
                            fs.readdir(uploadPath, function(err, list_of_files) {
                                if (err) throw err;
                                list_of_files.map(function (file) {
                                    return path.join(uploadPath, file);
                                }).forEach(function(filename) {
                                    fs.unlink(filename);
                                    console.log("******* Cleaned up uploads *****");
                                });
                            });
                        }

                    });
                });

                var stream = fs.createReadStream(croppedNFTImagePath);
                stream.on('data', function(data) {
                    postReq.write(data);
                });
                stream.on('end', function() {
                    postReq.end();
                });

            } else {

                console.log("******* Error from opencv program *****");
                console.log(stderr);
                response.end('{"error" : "Error at opencv server", "status" : 500}');
            }

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
 
app.listen(port);
console.log('Node.js server running on port ' + port);