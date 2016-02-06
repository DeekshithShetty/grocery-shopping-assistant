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

bool isRepetitive(const string& s){
    int count = 0;

    for (int i=0; i<(int)s.size(); i++){
        if ((s[i] == 'i') ||
                (s[i] == 'l') ||
                (s[i] == 'I'))
            count++;
    }
    if (count > ((int)s.size()+1)/2){
        return true;
    }
    return false;
}

void er_draw(vector<Mat> &channels, vector<vector<ERStat> > &regions, vector<Vec2i> group, Mat& segmentation){
    
    for (int r=0; r<(int)group.size(); r++){
        ERStat er = regions[group[r][0]][group[r][1]];
        if (er.parent != NULL) { // deprecate the root region
            int newMaskVal = 255;
            int flags = 4 + (newMaskVal << 8) + FLOODFILL_FIXED_RANGE + FLOODFILL_MASK_ONLY;
            floodFill(channels[group[r][0]],segmentation,Point(er.pixel%channels[group[r][0]].cols,er.pixel/channels[group[r][0]].cols),
                      Scalar(255),0,Scalar(er.level),Scalar(0),flags);
        }
    }
}

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

    //image = enhanceImage(image);
    cvtColor(image, grey, CV_BGR2GRAY);
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


    return grey;
}


void endToEndSceneTextDetectionAndRecognition(Mat image){

    /*Text Detection*/

    //cvtColor(grey,grey,COLOR_RGB2GRAY);
    Mat grey;

    grey = labelPreprocessing(image);

    // Extract channels to be processed individually
    vector<Mat> channels,channels2;

    if (grey.channels() == 3){
        cvtColor(grey, grey, CV_BGR2GRAY);
    }

    computeNMChannels(image, channels2);
    // Append negative channels to detect ER- (bright regions over dark background)
    //for (int c = 0; c < cn-1; c++)
        //channels.push_back(255-channels[c]);

    // Notice here we are only using grey channel
    channels.push_back(grey);
    channels.push_back(255-grey);
    //channels.push_back(channels2[1]);
    //channels.push_back(channels2[3]);

    cout << "image.getMat().type() :" << image.type() << endl;
    image = grey;
    //image.convertTo(image, CV_8UC3);
    //image.convertTo(image, 16);
    cvtColor(image, image, CV_GRAY2RGB);
    cout << "image.getMat().type() :" << image.type() << endl;

    double t_d = (double)getTickCount();
    // Create ERFilter objects with the 1st and 2nd stage default classifiers
    //Ptr<ERFilter> er_filter1 = createERFilterNM1(loadClassifierNM1("trained_classifierNM1.xml"),8,0.00015f,0.13f,0.2f,true,0.1f);
    //Ptr<ERFilter> er_filter2 = createERFilterNM2(loadClassifierNM2("trained_classifierNM2.xml"),0.5);
    Ptr<ERFilter> er_filter1 = createERFilterNM1(loadClassifierNM1("trained_classifierNM1.xml"),8,0.00005f,0.23f,0.2f,true,0.1f);
    Ptr<ERFilter> er_filter2 = createERFilterNM2(loadClassifierNM2("trained_classifierNM2.xml"),0.5);

    vector<vector<ERStat> > regions(channels.size());
    // Apply the default cascade classifier to each independent channel (could be done in parallel)
    for (int c=0; c<(int)channels.size(); c++)
    {
        er_filter1->run(channels[c], regions[c]);
        er_filter2->run(channels[c], regions[c]);
    }
    cout << "TIME_REGION_DETECTION = " << ((double)getTickCount() - t_d)*1000/getTickFrequency() << endl;

    Mat out_img_decomposition= Mat::zeros(image.rows+2, image.cols+2, CV_8UC1);
    vector<Vec2i> tmp_group;
    for (int i=0; i<(int)regions.size(); i++)
    {
        for (int j=0; j<(int)regions[i].size();j++)
        {
            tmp_group.push_back(Vec2i(i,j));
        }
        Mat tmp= Mat::zeros(image.rows+2, image.cols+2, CV_8UC1);
        //er_draw creates a mask for the group tmp_group in regions
        er_draw(channels, regions, tmp_group, tmp);
        if (i > 0)
            tmp = tmp / 2;
        out_img_decomposition = out_img_decomposition | tmp;
        tmp_group.clear();
    }
    imshow( "out_img_decomposition", out_img_decomposition);

    double t_g = (double)getTickCount();
    // Detect character groups
    vector< vector<Vec2i> > nm_region_groups;
    vector<Rect> nm_boxes;
    erGrouping(image, channels, regions, nm_region_groups, nm_boxes,ERGROUPING_ORIENTATION_HORIZ);
    cout << "TIME_GROUPING = " << ((double)getTickCount() - t_g)*1000/getTickFrequency() << endl;

    /*Text Recognition (OCR)*/

    double t_r = (double)getTickCount();
    Ptr<OCRTesseract> ocr = OCRTesseract::create(NULL,NULL,"0123456789",tesseract::OEM_CUBE_ONLY,tesseract::PSM_AUTO);
    cout << "TIME_OCR_INITIALIZATION = " << ((double)getTickCount() - t_r)*1000/getTickFrequency() << endl;
    string output;

    Mat out_img;
    Mat out_img_detection;
    Mat out_img_segmentation = Mat::zeros(image.rows+2, image.cols+2, CV_8UC1);
    image.copyTo(out_img);
    image.copyTo(out_img_detection);
    float scale_img  = 600.f/image.rows;
    float scale_font = (float)(2-scale_img)/1.4f;
    vector<string> words_detection;

    t_r = (double)getTickCount();

    for (int i=0; i<(int)nm_boxes.size(); i++){
        rectangle(out_img_detection, nm_boxes[i].tl(), nm_boxes[i].br(), Scalar(0,255,255), 3);
        imshow( "out_img_detection", out_img_detection);
        cout << "nm_boxes[i]" << nm_boxes[i].tl() << nm_boxes[i].br() << endl;
        //nm_boxes[i].br().x += 50;

        Mat group_img = Mat::zeros(image.rows+2, image.cols+2, CV_8UC1);
        //er_draw creates a mask for the group nm_region_groups[i] in regions
        er_draw(channels, regions, nm_region_groups[i], group_img);

        //test
        vector<Vec2i> vec;
        //for (auto vec : nm_region_groups[i]){
        //    std::cout << vec << std::endl;
        //}
        if(i == 0)
            imshow( "group_img1", group_img);

        Mat group_segmentation;
        group_img.copyTo(group_segmentation);
        //image(nm_boxes[i]).copyTo(group_img);
        group_img(nm_boxes[i]).copyTo(group_img);
        if(i == 0)
            imshow( "group_img2", group_img);
        copyMakeBorder(group_img,group_img,15,15,15,15,BORDER_CONSTANT,Scalar(0));
        if(i == 0)
            imshow( "group_img3", group_img);

        vector<Rect>   boxes;
        vector<string> words;
        vector<float>  confidences;
        ocr->run(group_img, output, &boxes, &words, &confidences, OCR_LEVEL_TEXTLINE);

        output.erase(remove(output.begin(), output.end(), '\n'), output.end());
        cout << "OCR output = \"" << output << endl;
        if (output.size() < 3)
            continue;

        for (int j=0; j<(int)boxes.size(); j++)
        {
            boxes[j].x += nm_boxes[i].x-15;
            boxes[j].y += nm_boxes[i].y-15;

            //cout << "  word = " << words[j] << "\t confidence = " << confidences[j] << endl;
            if ((words[j].size() < 2) || (confidences[j] < 51) ||
                    ((words[j].size()==2) && (words[j][0] == words[j][1])) ||
                    ((words[j].size()< 4) && (confidences[j] < 60)) ||
                    isRepetitive(words[j]))
                continue;
            words_detection.push_back(words[j]);
            rectangle(out_img, boxes[j].tl(), boxes[j].br(), Scalar(255,0,255),3);
            Size word_size = getTextSize(words[j], FONT_HERSHEY_SIMPLEX, (double)scale_font, (int)(3*scale_font), NULL);
            rectangle(out_img, boxes[j].tl()-Point(3,word_size.height+3), boxes[j].tl()+Point(word_size.width,0), Scalar(255,0,255),-1);
            //putText(out_img, words[j], boxes[j].tl()-Point(1,1), FONT_HERSHEY_SIMPLEX, scale_font, Scalar(255,255,255),(int)(3*scale_font));
            //cout << "WORD = " << words[j] << endl;
            out_img_segmentation = out_img_segmentation | group_segmentation;
            imshow( "out_img_segmentation", out_img_segmentation);
            imshow( "out_img", out_img);
        }

    }
    imwrite("1-table_image.jpg", image);
    imwrite("2-decomposition.jpg", out_img_decomposition);
    imwrite("3-detection.jpg", out_img_detection);
    imwrite("4-segmentation.jpg", out_img_segmentation);
    imwrite("5-out_image.jpg", out_img);
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
        //Ptr<OCRTesseract> ocr = OCRTesseract::create(NULL,NULL,"0123456789",tesseract::OEM_CUBE_ONLY,tesseract::PSM_AUTO);
        Ptr<OCRTesseract> ocr = OCRTesseract::create();
        //rois[i] = labelPreprocessing(rois[i]);
        cout << "rois[i].channels() = " << rois[i].channels() << endl;
        Mat gray;
        if (rois[i].channels() == 3){
            cvtColor(rois[i], gray, CV_BGR2GRAY);
        } else {
            gray = rois[i];
        }
        cv::threshold(gray, gray, 128, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);
        // Apply Histogram Equalization
        equalizeHist( gray, gray );
        cv::imshow("enhanced label image", gray);


        //endToEndSceneTextDetectionAndRecognition(rois[i]);

        //Detect
        std::vector<cv::Rect> letterBBoxes1 = detectLetters(rois[i]);
        //Display bounding boxes of detected words 
        for(int j=0; j< letterBBoxes1.size(); j++){
            //void rectangle(Mat& img, Point pt1, Point pt2, const Scalar& color, int thickness=1, int lineType=8, int shift=0)
            //void rectangle(Mat& img, Rect rec, const Scalar& color, int thickness=1, int lineType=8, int shift=0 )
            cv::rectangle(rois[i],letterBBoxes1[j],cv::Scalar(0,255,0),2,8,0);

            //test
            Mat group_img = Mat::zeros(gray.rows+2, gray.cols+2, CV_8UC1);;
            group_img = gray(letterBBoxes1[j]);
            if(j == 0)
                imshow( "group_img1", group_img);
            copyMakeBorder(group_img,group_img,10,10,10,10,BORDER_CONSTANT,Scalar(0));
            if(j == 0)
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

