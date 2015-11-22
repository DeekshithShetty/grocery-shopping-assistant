
//Module dependencies
var express  = require('express');
var bodyParser = require('body-parser');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
var fs = require('fs');
var multer = require('multer');
var app      = express();
var path    = require("path");

var fileUpload_done = false;
var port     = process.env.PORT || 8080;

//Multer configurations for image upload 
app.use(multer({ dest: './uploads/',
        rename: function (fieldname, filename) {
            return filename + "-" + Date.now();
        },
        onFileUploadStart: function (file) {
            console.log(file.originalname + ' is starting ...');
        },
        onFileUploadComplete: function (file) {
            console.log(file.fieldname + ' uploaded to  ' + file.path);
            fileUpload_done = true;
        }
}));


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

/*
//testing upload
app.post('/upload23', function(req, res) {
    console.log(req.files.image.originalFilename);
    console.log(req.files.image.path);
    fs.readFile(req.files.image.path, function (err, data){
        var dirname = ".";
        var newPath = dirname + "/uploads/" +   req.files.image.originalFilename;
        fs.writeFile(newPath, data, function (err) {
            if(err){
                res.json({'response':"Error"});
            }else {
                res.json({'response':"Saved"});
            }
        });
    });
});
*/

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
 
app.get('/uploads/:file', function (req, res){
    file = req.params.file;
    var dirname = "C:/Users/deeks/Desktop/Node.js/Node-Android";
    var img = fs.readFileSync(dirname + "/uploads/" + file);
    res.writeHead(200, {'Content-Type': 'image/jpg' });
    res.end(img, 'binary');
 
});
 
app.listen(port);
console.log('Node.js server running on port ' + port);