#include "opencv2/opencv_modules.hpp"
#include <iostream>

#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/xfeatures2d.hpp"

using namespace std;
using namespace cv;

string similarImageName = "";
bool isFoundSimilar = false;

Mat img_1_final, img_2_final;
std::vector<KeyPoint> keypoints_1_final, keypoints_2_final;
std::vector< DMatch > good_matches_final;

int bestMatchSize = 50;

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

  img_1_final = imread( argvStrings[1].c_str(), CV_LOAD_IMAGE_GRAYSCALE );

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

      img_2_final = img_2;
      keypoints_1_final = keypoints_1;
      keypoints_2_final = keypoints_2;
      good_matches_final = good_matches;

      bestMatchSize = good_matches.size();
      similarImageName = argvStrings[i];
    }

  }

  if(bestMatchSize > 50){
    isFoundSimilar = true;
    //-- Draw only "good" matches
    Mat img_matches;
    drawMatches( img_1_final, keypoints_1_final, img_2_final, keypoints_2_final,
                 good_matches_final, img_matches, Scalar::all(-1), Scalar::all(-1),
                 std::vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS );

    //-- Show detected matches
    imshow( "Good Matches", img_matches );

    cout << "--- The pic with similar feature is " << similarImageName << endl;

  }
  
  waitKey(0);

  return 0;
}
