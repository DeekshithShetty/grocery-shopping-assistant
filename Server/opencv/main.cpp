#include <iostream>
#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

std::vector<cv::Rect> detectLetters(cv::Mat img)
{
    std::vector<cv::Rect> boundRect;
    cv::Mat img_gray, img_sobel, img_threshold, element;
    cvtColor(img, img_gray, CV_BGR2GRAY);
    cv::Sobel(img_gray, img_sobel, CV_8U, 1, 0, 3, 1, 0, cv::BORDER_DEFAULT);
    cv::threshold(img_sobel, img_threshold, 0, 255, CV_THRESH_OTSU+CV_THRESH_BINARY);
    element = getStructuringElement(cv::MORPH_RECT, cv::Size(17, 3) );
    cv::morphologyEx(img_threshold, img_threshold, CV_MOP_CLOSE, element); //Does the trick
    std::vector< std::vector< cv::Point> > contours;
    cv::findContours(img_threshold, contours, 0, 1); 
    std::vector<std::vector<cv::Point> > contours_poly( contours.size() );
    for( int i = 0; i < contours.size(); i++ )
        if (contours[i].size()>100)
        { 
            cv::approxPolyDP( cv::Mat(contours[i]), contours_poly[i], 3, true );
            cv::Rect appRect( boundingRect( cv::Mat(contours_poly[i]) ));
            if (appRect.width>appRect.height) 
                boundRect.push_back(appRect);
        }
    return boundRect;
}


int main(int argc, const char * argv[]){

    // Load source image
    Mat src = imread(argv[1]);

    // Check if image is loaded fine
    if(!src.data)
        cerr << "Problem loading image!!!" << endl;

    // Show source image
    //imshow("src", src);

    // resizing for practical reasons
    Mat rsz;
    Size size(800, 900);
    resize(src, rsz, size);
    imshow("resize", rsz);

    // Transform source image to gray if it is not and show grayscale image
    Mat gray;
    if (rsz.channels() == 3){
        cvtColor(rsz, gray, CV_BGR2GRAY);
    } else {
        gray = rsz;
    }
    imshow("grayscale", gray);

    // Apply adaptiveThreshold at the bitwise_not of gray, notice the ~ symbol
    Mat bw;
    adaptiveThreshold(~gray, bw, 255, CV_ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, -2);
    // Show binary image
    imshow("binary", bw);

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
    imshow("horizontal", horizontal);

    // Specify size on vertical axis
    int verticalsize = vertical.rows / scale;
    // Create structure element for extracting vertical lines through morphology operations
    Mat verticalStructure = getStructuringElement(MORPH_RECT, Size( 1,verticalsize));
    // Apply morphology operations
    erode(vertical, vertical, verticalStructure, Point(-1, -1));
    dilate(vertical, vertical, verticalStructure, Point(-1, -1));
    //dilate(vertical, vertical, verticalStructure, Point(-1, -1)); // expand vertical lines
    // Show extracted vertical lines
    imshow("vertical", vertical);

    // create a mask which includes the tables
    Mat mask = horizontal + vertical;
    imshow("mask", mask);

    // find the joints between the lines of the tables, we will use this information in order
    // to descriminate tables from pictures (tables will contain more than 4 joints while
    // a picture only 4 (i.e. at the corners))
    Mat joints;
    bitwise_and(horizontal, vertical, joints);
    imshow("joints", joints);

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

    for(size_t i = 0; i < rois.size(); ++i){

        // Now you can do whatever post process you want
        // with the data within the rectangles/tables.
        imshow("roi", rois[i]);

    //Extra Draw Rectangle

        //Detect
        std::vector<cv::Rect> letterBBoxes1 = detectLetters(rois[i]);
        //Display
        for(int j=0; j< letterBBoxes1.size(); j++)
            //void rectangle(Mat& img, Point pt1, Point pt2, const Scalar& color, int thickness=1, int lineType=8, int shift=0)
            //void rectangle(Mat& img, Rect rec, const Scalar& color, int thickness=1, int lineType=8, int shift=0 )
            cv::rectangle(rois[i],letterBBoxes1[j],cv::Scalar(0,255,0),3,8,0);
        //cv::imwrite( "imgOut1.jpg", img1);
        imshow( "Rectangle", rois[i]);
    
    //Extra Draw Rectangle ends  


/*
//Extra Code
        cvtColor(rois[i], src, CV_BGR2GRAY);

        vector<vector<Point> > contours;
        vector<Vec4i> hierarchy;

        Canny( src, src, 100, 200, 3 );
        imshow("g",src);

        findContours( src, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );

        Mat drawing = Mat::zeros( src.size(), CV_8UC3 );
        char text[200] = "";
        CvFont font = cvFont(2,2);

        for( int i = 0; i< contours.size(); i++ )
        {
        drawContours( drawing, contours, i, CV_RGB(255,0,0), 2, 8, hierarchy, 0, Point() );
        cv::Rect brect = cv::boundingRect(contours[i]);
        if (brect.area() < 1000)
            continue;
        sprintf(text,"S = %d", brect.area());
        putText(drawing, text, cvPoint(brect.x+20, brect.y+20), 1, 1, CV_RGB(0,255,0));
        rectangle(drawing, brect, CV_RGB(0,0,255), 3);
        }


        imshow( "Contours", drawing);
//Extra Code ends
*/


        waitKey();
    }

    imshow("contours", rsz);

    waitKey();
    return 0;
}