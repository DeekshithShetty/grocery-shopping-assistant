#include <iostream>
#include "opencv2/text.hpp"
#include <opencv2/opencv.hpp>
#include <tesseract/baseapi.h>
#include <tesseract/strngs.h>
#include <string>
#include <regex>
#include <sstream> 

using namespace std;
using namespace cv;
using namespace cv::text;

std::vector<Mat> extractTableFromImage(Mat src){

    // resizing for practical reasons
    Mat rsz;
    Size size(800, 900);
    resize(src, rsz, size);

    // Transform source image to gray if it is not and show grayscale image
    Mat gray;
    if (rsz.channels() == 3){
        cvtColor(rsz, gray, CV_BGR2GRAY);
    } else {
        gray = rsz;
    }

    // Apply adaptiveThreshold at the bitwise_not of gray, notice the ~ symbol
    Mat bw;
    adaptiveThreshold(~gray, bw, 255, CV_ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, -2);

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

    // Specify size on vertical axis
    int verticalsize = vertical.rows / scale;
    // Create structure element for extracting vertical lines through morphology operations
    Mat verticalStructure = getStructuringElement(MORPH_RECT, Size( 1,verticalsize));
    // Apply morphology operations
    erode(vertical, vertical, verticalStructure, Point(-1, -1));
    dilate(vertical, vertical, verticalStructure, Point(-1, -1));

    // create a mask which includes the tables
    Mat mask = horizontal + vertical;

    // find the joints between the lines of the tables, we will use this information in order
    // to descriminate tables from pictures (tables will contain more than 4 joints while
    // a picture only 4 (i.e. at the corners))
    Mat joints;
    bitwise_and(horizontal, vertical, joints);

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

string nutrientinfo_regex_search(std::string& input){

    std::stringstream s;
    std::string jsonString;


    //std::regex rgx("[A-Z][a-z]+[ ][(]?[g|3]?[)]?[ ][0-9|O]+[a-z]*");
    std::regex rgx("[A-Z][a-z]+[ ][0-9|O]+[a-z]*");
    std::smatch match;

    std::smatch nut_match;
    //Carbohydrates (g) 70g
    std::regex carbRegex("[C][a][a-z]*[ ]([0-9|o|O]+[a-z]*)");
    std::regex sugarsRegex("[S][u][a-z]*[ ]([0-9|o|O]+[a-z]*)");
    std::regex proteinRegex("[P][r][a-z]*[ ]([0-9|o|O]+[a-z]*)");
    std::regex fatsRegex("[F][a][a-z]*[ ]([0-9|o|O]+[a-z]*)");

    std::smatch nut_match2;

    regex regexOto0("[O]");

    double carbsValue;
    double sugarsValue;
    double proteinValue;
    double fatsValue;

    while (std::regex_search(input, match, rgx)){
        //std::cout << "Match : " << match[0] << endl;

        //for (auto m : match)
          //std::cout << "  submatch " << m << '\n';

        if(std::regex_search(match[0].str(), nut_match, carbRegex)){

           //std::cout << "\tIts Carbohydrates" << endl;
           //std::cout << "\tOld Carbohydrates = " << nut_match[1].str() << endl;

           string OremovedString = regex_replace(nut_match[1].str(), regexOto0, "0");

           std::regex regex3("([0-9]+)[3]");
           std::regex regexTh("([0-9]+)[t][h]");

           if(std::regex_search(OremovedString, nut_match2, regex3)){

                carbsValue = ::atof(nut_match2[1].str().c_str());
                //std::cout << "\tNew Carbohydrates = " << carbsValue << "g" << endl;
           } else if(std::regex_search(OremovedString, nut_match2, regexTh)){

                regex thRegex("[t][h]");
                string thRelpacedString = regex_replace(OremovedString, thRegex, "5");
                carbsValue = ::atof(thRelpacedString.c_str());
                //std::cout << "\tNew Carbohydrates = " << carbsValue << "g" << endl;
           } else {
                regex regexG("[g]");
                string GremovedString = regex_replace(OremovedString, regexG, "");
                carbsValue = ::atof(GremovedString.c_str());
                //std::cout << "\tNew Carbohydrates = " << carbsValue << "g" << endl;
           }

        }else if (std::regex_search(match[0].str(), nut_match, sugarsRegex)){

            //std::cout << "\tIts Sugars" << endl;
            //std::cout << "\tOld Sugars = " << nut_match[1].str() << endl;

            string OremovedString = regex_replace(nut_match[1].str(), regexOto0, "0");

            std::regex regex3("([0-9]+)[3]");
            std::regex regexTh("([0-9]+)[t][h]");

            if(std::regex_search(OremovedString, nut_match2, regex3)){

                sugarsValue = ::atof(nut_match2[1].str().c_str());
                //std::cout << "\tNew Sugars = " << sugarsValue << "g" << endl;
            } else if(std::regex_search(OremovedString, nut_match2, regexTh)){

                regex thRegex("[t][h]");
                string thRelpacedString = regex_replace(OremovedString, thRegex, "5");
                sugarsValue = ::atof(thRelpacedString.c_str());
                //std::cout << "\tNew Sugars = " << sugarsValue << "g" << endl;
            } else {
                regex regexG("[g]");
                string GremovedString = regex_replace(OremovedString, regexG, "");
                sugarsValue = ::atof(GremovedString.c_str());
                //std::cout << "\tNew Sugars = " << sugarsValue << "g" << endl;
            }

        }else if (std::regex_search(match[0].str(), nut_match, proteinRegex)){

            //std::cout << "\tIts Protein" << endl;
            //std::cout << "\tOld Protein = " << nut_match[1].str() << endl;

            string OremovedString = regex_replace(nut_match[1].str(), regexOto0, "0");

            std::regex regex3("([0-9]+)[3]");
            std::regex regexTh("([0-9]+)[t][h]");

            if(std::regex_search(OremovedString, nut_match2, regex3)){

                proteinValue = ::atof(nut_match2[1].str().c_str());
                //std::cout << "\tNew Protein = " << proteinValue << "g" << endl;
            } else if(std::regex_search(OremovedString, nut_match2, regexTh)){

                regex thRegex("[t][h]");
                string thRelpacedString = regex_replace(OremovedString, thRegex, "5");
                proteinValue = ::atof(thRelpacedString.c_str());
                //std::cout << "\tNew Protein = " << proteinValue << "g" << endl;
            } else {
                regex regexG("[g]");
                string GremovedString = regex_replace(OremovedString, regexG, "");
                proteinValue = ::atof(GremovedString.c_str());
                //std::cout << "\tNew Protein = " << proteinValue << "g" << endl;
            }
            
        }else if (std::regex_search(match[0].str(), nut_match, fatsRegex)){

            //std::cout << "\tIts Fats" << endl;
            //std::cout << "\tOld Fats = " << nut_match[1].str() << endl;

            string OremovedString = regex_replace(nut_match[1].str(), regexOto0, "0");

            std::regex regex3("([0-9]+)[3]");
            std::regex regexTh("([0-9]+)[t][h]");

            if(std::regex_search(OremovedString, nut_match2, regex3)){

                fatsValue = ::atof(nut_match2[1].str().c_str());
                //std::cout << "\tNew Fats = " << fatsValue << "g" << endl;
            } else if(std::regex_search(OremovedString, nut_match2, regexTh)){

                regex thRegex("[t][h]");
                string thRelpacedString = regex_replace(OremovedString, thRegex, "5");
                fatsValue = ::atof(thRelpacedString.c_str());
                //std::cout << "\tNew Fats = " << fatsValue << "g" << endl;
            } else {
                regex regexG("[g]");
                string GremovedString = regex_replace(OremovedString, regexG, "");
                fatsValue = ::atof(GremovedString.c_str());
                //std::cout << "\tNew Fats = " << fatsValue << "g" << endl;
            }
            
        }
         input = match.suffix().str();

    }

    s << "{\
    \"Carbohydrates\" : " << carbsValue << ",\
    \"Sugars\" : " << sugarsValue << ",\
    \"Protein\" : " << proteinValue << ",\
    \"Fats\" : " << fatsValue << "\
    }";
    jsonString = s.str();

    return jsonString;            

}

int main(int argc, const char * argv[]){

    std::string jsonNutrients;
    // Load source image
    Mat src = imread(argv[1]);

    // Check if image is loaded fine
    if(!src.data)
        cerr << "Problem loading image!!!" << endl;

    vector<Mat> rois = extractTableFromImage(src);
    
    //Extra Draw Rectangle
    //Ptr<OCRTesseract> ocr = OCRTesseract::create(NULL,NULL,"Carbohydrates.mg0123456789",tesseract::OEM_CUBE_ONLY,tesseract::PSM_AUTO);
    Ptr<OCRTesseract> ocr = OCRTesseract::create();
    Mat gray;
    gray = labelPreprocessing(rois[0]);
    string fullOutputBW;

    ocr->run(gray, fullOutputBW, NULL, NULL, NULL, OCR_LEVEL_TEXTLINE);
    fullOutputBW.erase(remove(fullOutputBW.begin(), fullOutputBW.end(), '\n'), fullOutputBW.end());
    //cout << "OCR fullOutputBW = \"" << fullOutputBW << "\""<< endl;

    jsonNutrients = nutrientinfo_regex_search(fullOutputBW);

    cout << " { \"jsonNutrients\" : " << jsonNutrients << "}" << endl;

    return 0;
}

