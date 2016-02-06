#include <iostream>
#include "opencv2/text.hpp"
#include <opencv2/opencv.hpp>
#include <tesseract/baseapi.h>
#include <tesseract/strngs.h>

using namespace std;
using namespace cv;
using namespace cv::text;

void sortRectangles(vector<cv::Rect> rects){
    // sort by Y
    std::sort(rects.begin(), rects.end(), [] (const CvRect& r1, const CvRect& r2) -> bool {
          
        return r1.y < r2.y;  
          
    });

    int pos_start = 0;

    for (int i=1; i<rects.size(); i++) {

        // detect next row
        if (abs(rects[i].y-rects[i-1].y) > 35) { // border value = 35
            std::sort(rects.begin()+pos_start, rects.begin()+i, [] (const CvRect& r1, const CvRect& r2) -> bool {

                return r1.x < r2.x;

            });
            pos_start = i;
            i++;
        }

    }
 
    // sorting last row
    std::sort(rects.begin()+pos_start, rects.end(), [] (const CvRect& r1, const CvRect& r2) -> bool {

        return r1.x < r2.x;

    });
}


int main(int argc, const char * argv[]){

    // Load source image
    Mat src = imread(argv[1]);

    // Check if image is loaded fine
    if(!src.data)
        cerr << "Problem loading image!!!" << endl;

    cvtColor(src, src, CV_BGR2GRAY);

    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;

    Canny( src, src, 100, 200, 3 );
    imshow("canny",src);

    findContours( src, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );

    Mat drawing = Mat::zeros( src.size(), CV_8UC3 );
    char text[200] = "";
    CvFont font = cvFont(2,2);

    std::vector<cv::Rect> boundRect;

    for( int i = 0; i< contours.size(); i++ ){
        drawContours( drawing, contours, i, CV_RGB(255,0,0), 2, 8, hierarchy, 0, Point() );
        cv::Rect brect = cv::boundingRect(contours[i]);

        if (brect.area() < 1000)
            continue;

        sprintf(text,"S = %d", brect.area());
        putText(drawing, text, cvPoint(brect.x+20, brect.y+20), 1, 1, CV_RGB(0,255,0));
        rectangle(drawing, brect, CV_RGB(0,0,255), 3);

        boundRect.push_back(brect);
    }

    boundRect = sortRectangles(boundRect);


    imshow( "Contours", drawing);

    Ptr<OCRTesseract> ocr = OCRTesseract::create();

    //Display bounding boxes of detected words 
    for(int j=0; j< boundRect.size(); j++){

        cv::rectangle(src,boundRect[j],cv::Scalar(0,255,0),3,8,0);

        //test
        Mat group_img = Mat::zeros(src.rows+2, src.cols+2, CV_8UC1);;
        group_img = src(boundRect[j]);
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


    waitKey();
    return 0;   
}

