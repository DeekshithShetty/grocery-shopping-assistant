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
  bool gotSimilarImage;

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

    gotSimilarImage = checkSimilarityInImages(image1, image2,argvStrings[2],argvStrings[i]);

    if(gotSimilarImage){
      isFoundSimilar = true;
      //showMatches(img_1, img_2, keypoints_1, keypoints_2);
      break;
    }
  }

  s << "{\
    \"isFoundSimilar\" : " << isFoundSimilar << ",\
    \"imageName\" : \"" << similarImageName << "\"\
    }";

  cout << s.str() << endl;

  //waitKey(0);

  return 0;
}


bool checkSimilarityInImages(float image1, float image2, string imageName1, string imageName2){
  if(image1 > image2){

    if(image2 > (image1 * 0.75)){

      similarImageName = imageName2;
      return true;
    } 

  }else if (image2 > image1){

    if(image1 > (image2 * 0.75)){

      similarImageName = imageName2;
      return true;
    } 
  }else if (image1 == image2){

    similarImageName = imageName2;
    return true;
  }

  return false;
}

void showMatches(Mat img_1, Mat img_2, std::vector<KeyPoint> keypoints_1, std::vector<KeyPoint> keypoints_2){
  //-- Step 2: Calculate descriptors (feature vectors)
  Ptr<xfeatures2d::SURF> extractor = xfeatures2d::SURF::create(); // note extra namespace
  //SurfDescriptorExtractor extractor;

  Mat descriptors_1, descriptors_2;

  extractor->compute( img_1, keypoints_1, descriptors_1 );
  extractor->compute( img_2, keypoints_2, descriptors_2 );

  //-- Step 3: Matching descriptor vectors using FLANN matcher
  FlannBasedMatcher matcher;
  std::vector< DMatch > matches;
  matcher.match( descriptors_1, descriptors_2, matches );

  double max_dist = 0; double min_dist = 100;

  //-- Quick calculation of max and min distances between keypoints
  for( int i = 0; i < descriptors_1.rows; i++ )
  { double dist = matches[i].distance;
    if( dist < min_dist ) min_dist = dist;
    if( dist > max_dist ) max_dist = dist;
  }

  //-- Draw only "good" matches (i.e. whose distance is less than 2*min_dist,
  //-- or a small arbitary value ( 0.02 ) in the event that min_dist is very
  //-- small)
  //-- PS.- radiusMatch can also be used here.
  std::vector< DMatch > good_matches;

  for( int i = 0; i < descriptors_1.rows; i++ )
  { if( matches[i].distance <= max(2*min_dist, 0.02) )
    { good_matches.push_back( matches[i]); }
  }

  //-- Draw only "good" matches
  Mat img_matches;
  drawMatches( img_1, keypoints_1, img_2, keypoints_2,
               good_matches, img_matches, Scalar::all(-1), Scalar::all(-1),
               std::vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS );

  //-- Show detected matches
  imshow( "Good Matches", img_matches );
}
