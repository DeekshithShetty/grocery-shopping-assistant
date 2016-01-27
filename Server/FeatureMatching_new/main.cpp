#include "opencv2/opencv_modules.hpp"
#include <iostream>

#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/xfeatures2d.hpp"
#include <sstream> 

using namespace std;
using namespace cv;

string similarImageName = "";
bool isFoundSimilar = false;

int bestMatchSize = 50;

bool checkSimilarityInImages(float image1, float image2, string imageName1, string imageName2);
void showMatches(Mat img_1, Mat img_2, std::vector<KeyPoint> keypoints_1, std::vector<KeyPoint> keypoints_2);

int main( int argc, char** argv ){

  std::stringstream s;

  if( argc < 3 ){ 
    cout << " Usage: ./FeatureMatching <img1> <img2> .." << endl;
    return -1;
  }

  string argvStrings[argc];
  argvStrings[0] = string(argv[0]); //program name
  //argvStrings[1] = string(argv[1]); //user folder name

  argvStrings[1] = string(argv[1]); //image that is to be checked 

  for(int i = 2; i < argc; i++){
    //argvStrings[i] = argvStrings[1] + string(argv[i]);
    argvStrings[i] = string(argv[i]);
  }

  Mat img_1, img_2;
  int i;
  float image1,image2;
  int minHessian = 400;

  Ptr<xfeatures2d::SURF> detector = xfeatures2d::SURF::create( minHessian );

  for(i = 2; i < argc; i++){
    img_1 = imread( argvStrings[1].c_str(), CV_LOAD_IMAGE_GRAYSCALE );
    img_2 = imread( argvStrings[i].c_str(), CV_LOAD_IMAGE_GRAYSCALE );

    if( !img_1.data || !img_2.data ){ 
      cout << " --(!) Error reading images" << endl;
      return -1; 
    } 

    std::vector<KeyPoint> keypoints_1, keypoints_2;

    detector->detect( img_1, keypoints_1 );
    detector->detect( img_2, keypoints_2 );

    image1 = keypoints_1.size();
    image2 = keypoints_2.size();

    //-- Step 2: Calculate descriptors (feature vectors)
    Ptr<xfeatures2d::SURF> extractor = xfeatures2d::SURF::create(); // note extra namespace
    //SurfDescriptorExtractor extractor;

    Mat descriptors_1, descriptors_2;

    extractor->compute( img_1, keypoints_1, descriptors_1 );
    extractor->compute( img_2, keypoints_2, descriptors_2 );

    //-- Step 3: Matching descriptor vectors using FLANN matcher
    std::vector< std::vector< DMatch> > matches;
    FlannBasedMatcher matcher;
    //cv::BFMatcher matcher;
    matcher.knnMatch(descriptors_1, descriptors_2, matches, 2);  // Find two nearest matches

    std::vector< DMatch > good_matches;

    for (int j = 0; j < matches.size(); ++j){
        const float ratio = 0.8; // As in Lowe's paper; can be tuned
        if (matches[j][0].distance < ratio * matches[j][1].distance){
            good_matches.push_back(matches[j][0]);
        }
    }

    if(bestMatchSize < good_matches.size()){
      bestMatchSize = good_matches.size();
      similarImageName = argvStrings[i];
    }

    //printf("Good Matches : %d\n", good_matches.size());

    
  }
  if(bestMatchSize > 50){
    isFoundSimilar = true;
  }

  s << "{\
    \"isFoundSimilar\" : " << isFoundSimilar << ",\
    \"imageName\" : \"" << similarImageName << "\",\
    \"goodMatches\" : " << bestMatchSize << "\
    }";

  cout << s.str() << endl;

  return 0;
}

