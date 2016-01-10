#include <iostream>
#include "opencv2/text.hpp"
#include <opencv2/opencv.hpp>
#include <tesseract/baseapi.h>
#include <tesseract/strngs.h>

using namespace std;
using namespace cv;
using namespace cv::text;

std::vector<cv::Rect> detectLetters(cv::Mat img){

    std::vector<cv::Rect> boundRect;
    cv::Mat img_gray, img_sobel, img_threshold, element;
    if (img.channels() == 3){
        cvtColor(img, img_gray, CV_BGR2GRAY);
    } else {
        img_gray = img;
    }
    //cvtColor(img, img_gray, CV_BGR2GRAY);
    cv::Sobel(img_gray, img_sobel, CV_8U, 1, 0, 3, 1, 0, cv::BORDER_DEFAULT);
    cv::threshold(img_sobel, img_threshold, 0, 255, CV_THRESH_OTSU+CV_THRESH_BINARY);
    //cv::imshow("img_sobel", img_sobel);
    //cv::imshow("img_threshold1", img_threshold); 
    element = getStructuringElement(cv::MORPH_RECT, cv::Size(17, 3) );
    cv::morphologyEx(img_threshold, img_threshold, CV_MOP_CLOSE, element); //Does the trick
    std::vector< std::vector< cv::Point> > contours;
    cv::findContours(img_threshold, contours, 0, 1);
    //cv::imshow("img_threshold2", img_threshold); 
    std::vector<std::vector<cv::Point> > contours_poly( contours.size() );
    for( int i = 0; i < contours.size(); i++ )
        if (contours[i].size()>100)
        { 
            cv::approxPolyDP( cv::Mat(contours[i]), contours_poly[i], 3, true );
            cv::Rect appRect( boundingRect( cv::Mat(contours_poly[i]) ));
            if (appRect.width>appRect.height){
                appRect.x = 10;                               //Specific for hearts.jpg
                appRect.width = img_gray.size().width-30;     //Specific for hearts.jpg  
                //cout << "appRect = " << appRect << endl;
                boundRect.push_back(appRect);
            }
        }
    return boundRect;
}
/*
std::vector<cv::Rect> detectLetters2(cv::Mat img){

    std::vector<cv::Rect> boundRect;

    //Prepare the image for findContours
    if (img.channels() == 3){
        cvtColor(img, img_gray, CV_BGR2GRAY);
    } else {
        img_gray = img;
    }
    cv::threshold(image, image, 128, 255, CV_THRESH_BINARY);

    //Find the contours. Use the contourOutput Mat so the original image doesn't get overwritten
    std::vector<std::vector<cv::Point> > contours;
    cv::Mat contourOutput = image.clone();
    cv::findContours( contourOutput, contours, CV_RETR_LIST, CV_CHAIN_APPROX_NONE );

    //Draw the contours
    cv::Mat contourImage(image.size(), CV_8UC3, cv::Scalar(0,0,0));
    for (size_t idx = 0; idx < contours.size(); idx++) {
        cv::drawContours(contourImage, contours, idx, cv::Scalar(255,255,255));
    }

    cv::imshow("Input Image", image);
    cvMoveWindow("Input Image", 0, 0);
    cv::imshow("Contours", contourImage);
}
*/

std::vector<Mat> extractTableFromImage(Mat src){

    // resizing for practical reasons
    Mat rsz;
    Size size(800, 900);
    resize(src, rsz, size);
    //    imshow("resize", rsz);

    // Transform source image to gray if it is not and show grayscale image
    Mat gray;
    if (rsz.channels() == 3){
        cvtColor(rsz, gray, CV_BGR2GRAY);
    } else {
        gray = rsz;
    }
    //    imshow("grayscale", gray);

    // Apply adaptiveThreshold at the bitwise_not of gray, notice the ~ symbol
    Mat bw;
    adaptiveThreshold(~gray, bw, 255, CV_ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, -2);
    // Show binary image
    //    imshow("binary", bw);

    // Create the images that will use to extract the horizontal and vertical lines
    Mat horizontal = bw.clone();
    Mat vertical = bw.clone();

    int scale = 20; // play with this variable in order to increase/decrease the amount of lines to be detected

    // Specify size on horizontal axis
    int horizontalsize = horizontal.cols / scale;
    // Create structure element for extracting horizontal lines through morphology operations
    Mat horizontalStructure = getStructuringElement(MORPH_RECT, Size(horizontalsize,1));
    // Apply morphology operations, erosion removes white noises, but it also shrinks our object
    //So we dilate it, since noise is gone, they won't come back, but our object area increases
    erode(horizontal, horizontal, horizontalStructure, Point(-1, -1));
    dilate(horizontal, horizontal, horizontalStructure, Point(-1, -1));
    //dilate(horizontal, horizontal, horizontalStructure, Point(-1, -1)); // expand horizontal lines
    // Show extracted horizontal lines
    //    imshow("horizontal", horizontal);

    // Specify size on vertical axis
    int verticalsize = vertical.rows / scale;
    // Create structure element for extracting vertical lines through morphology operations
    Mat verticalStructure = getStructuringElement(MORPH_RECT, Size( 1,verticalsize));
    // Apply morphology operations
    erode(vertical, vertical, verticalStructure, Point(-1, -1));
    dilate(vertical, vertical, verticalStructure, Point(-1, -1));
    //dilate(vertical, vertical, verticalStructure, Point(-1, -1)); // expand vertical lines
    // Show extracted vertical lines
    //    imshow("vertical", vertical);

    // create a mask which includes the tables
    Mat mask = horizontal + vertical;
    //    imshow("mask", mask);

    // find the joints between the lines of the tables, we will use this information in order
    // to descriminate tables from pictures (tables will contain more than 4 joints while
    // a picture only 4 (i.e. at the corners))
    Mat joints;
    bitwise_and(horizontal, vertical, joints);
    //    imshow("joints", joints);

    // Find external contours from the mask, which most probably will belong to tables or to images
    vector<Vec4i> hierarchy;
    std::vector<std::vector<cv::Point> > contours;
    cv::findContours(mask, contours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0));

    vector<vector<Point> > contours_poly( contours.size() );
    vector<Rect> boundRect( contours.size() );
    vector<Mat> rois;

    for (size_t i = 0; i < contours.size(); i++){

        // find the area of each contour
        double area = contourArea(contours[i]);

        // filter individual lines of blobs that might exist and they do not represent a table
        if(area < 100) // value is randomly chosen, you will need to find that by yourself with trial and error procedure
            continue;

        approxPolyDP( Mat(contours[i]), contours_poly[i], 3, true );
        boundRect[i] = boundingRect( Mat(contours_poly[i]) );

        // find the number of joints that each table has
        Mat roi = joints(boundRect[i]);

        vector<vector<Point> > joints_contours;
        findContours(roi, joints_contours, RETR_CCOMP, CHAIN_APPROX_SIMPLE);

        // if the number is not more than 5 then most likely it not a table
        if(joints_contours.size() <= 4)
            continue;

        rois.push_back(rsz(boundRect[i]).clone());

        //drawContours( rsz, contours, i, Scalar(0, 0, 255), CV_FILLED, 8, vector<Vec4i>(), 0, Point() );
        rectangle( rsz, boundRect[i].tl(), boundRect[i].br(), Scalar(0, 255, 0), 1, 8, 0 );
    }

    return rois;
}

//Label pre processing
Mat labelPreprocessing(Mat image){
    Mat grey;

    if (image.channels() == 3){
        cvtColor(image, grey, CV_BGR2GRAY);
    } else {
        grey = image;
    }
    cv::threshold(grey, grey, 128, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);
    // Apply Histogram Equalization
    equalizeHist( grey, grey );
    //Mat kernel = getStructuringElement(MORPH_RECT, Size(2,2));
    //erode(grey, grey, kernel, Point(-1, -1));
    //dilate(grey, grey, kernel, Point(-1, -1));

    Mat kernel = getStructuringElement(MORPH_ELLIPSE, Size(19,19));
    Mat closed;
    morphologyEx( grey, closed, MORPH_CLOSE, kernel );

    grey.convertTo(grey, CV_32F); // divide requires floating-point
    divide(grey, closed, grey, 1, CV_32F);
    normalize(grey, grey, 0, 255, NORM_MINMAX, -1, noArray() );
    grey.convertTo(grey, CV_8UC1); // convert back to unsigned int

    cv::imshow("enhanced label image", grey);

    //rotating or skew
    Mat thr,dst;
    threshold(grey,thr,200,255,THRESH_BINARY_INV);
    std::vector<cv::Point> points;
    cv::Mat_<uchar>::iterator it = thr.begin<uchar>();
    cv::Mat_<uchar>::iterator end = thr.end<uchar>();
    for (; it != end; ++it)
        if (*it)
            points.push_back(it.pos());

    cv::RotatedRect box = cv::minAreaRect(cv::Mat(points));
    cv::Mat rot_mat = cv::getRotationMatrix2D(box.center, box.angle, 1);
    //cv::Mat rotated(src.size(),src.type(),Scalar(255,255,255));
    cv::warpAffine(grey, grey, rot_mat, grey.size(), cv::INTER_CUBIC);

    return grey;
}

int main(int argc, const char * argv[]){

    // Load source image
    Mat src = imread(argv[1]);

    // Check if image is loaded fine
    if(!src.data)
        cerr << "Problem loading image!!!" << endl;

    // Show source image
    //imshow("src", src);

    vector<Mat> rois = extractTableFromImage(src);
    
    for(size_t i = 0; i < rois.size(); ++i){

        // Now you can do whatever post process you want
        // with the data within the rectangles/tables.
        imshow("roi", rois[i]);

    //Extra Draw Rectangle
        //Ptr<OCRTesseract> ocr = OCRTesseract::create(NULL,NULL,"Carbohydrates.mg0123456789",tesseract::OEM_CUBE_ONLY,tesseract::PSM_AUTO);
        Ptr<OCRTesseract> ocr = OCRTesseract::create();
        //rois[i] = labelPreprocessing(rois[i]);
        Mat gray;
        gray = labelPreprocessing(rois[i]);
        string fullOutputRGB,fullOutputBW;
        ocr->run(rois[i], fullOutputRGB, NULL, NULL, NULL, OCR_LEVEL_TEXTLINE);
        fullOutputRGB.erase(remove(fullOutputRGB.begin(), fullOutputRGB.end(), '\n'), fullOutputRGB.end());
        cout << "OCR fullOutputRGB = \"" << fullOutputRGB << "\""<< endl;

        ocr->run(gray, fullOutputBW, NULL, NULL, NULL, OCR_LEVEL_TEXTLINE);
        fullOutputBW.erase(remove(fullOutputBW.begin(), fullOutputBW.end(), '\n'), fullOutputBW.end());
        cout << "OCR fullOutputBW = \"" << fullOutputBW << "\""<< endl;

        //Detect
        //std::vector<cv::Rect> letterBBoxes1 = detectLetters(gray);
        std::vector<cv::Rect> letterBBoxes1 = detectLetters(rois[i]);
        //Display bounding boxes of detected words 
        for(int j=0; j< letterBBoxes1.size(); j++){
            //void rectangle(Mat& img, Point pt1, Point pt2, const Scalar& color, int thickness=1, int lineType=8, int shift=0)
            //void rectangle(Mat& img, Rect rec, const Scalar& color, int thickness=1, int lineType=8, int shift=0 )
            cv::rectangle(rois[i],letterBBoxes1[j],cv::Scalar(0,255,0),3,8,0);

            //test
            Mat group_img = Mat::zeros(gray.rows+2, gray.cols+2, CV_8UC1);;
            group_img = gray(letterBBoxes1[j]);
            if(j == 5)
                imshow( "group_img1", group_img);
            //Top,Bottom,Left,Right
            copyMakeBorder(~group_img,group_img,2,2,100,3000,BORDER_CONSTANT,Scalar(0));
            //group_img = ~group_img;
            if(j == 5)
                imshow( "group_img2", group_img);

            vector<Rect>   boxes;
            vector<string> words;
            vector<float>  confidences;
            string output;
            ocr->run(group_img, output, &boxes, &words, &confidences, OCR_LEVEL_TEXTLINE);

            output.erase(remove(output.begin(), output.end(), '\n'), output.end());
            cout << "OCR output = \"" << output << endl;


        }

        //cv::imwrite( "imgOut1.jpg", img1);
        imshow( "Rectangle", rois[i]);

    //Extra Draw Rectangle ends  

        waitKey();
    }

    //imshow("contours", rsz);

    waitKey();
    return 0;
}

