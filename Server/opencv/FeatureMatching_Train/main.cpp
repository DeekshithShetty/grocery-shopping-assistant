#include "opencv2/core/core.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/xfeatures2d.hpp"

#include <iostream>
#include <fstream>
#include <sstream>
#include <time.h> 

using namespace cv;
using namespace std;

Mat img_1_final, img_2_final;
std::vector<KeyPoint> keypoints_1_final, keypoints_2_final;
std::vector< DMatch > good_matches_final;

string similarImageName = "";
bool isFoundSimilar = false;
int bestMatchSize = 0;
int currentMatchSize;


static void maskMatchesByTrainImgIdx( const vector<DMatch>& matches, int trainImgIdx, vector<char>& mask ){
    mask.resize( matches.size() );
    fill( mask.begin(), mask.end(), 0 );
    for( size_t i = 0; i < matches.size(); i++ ){
        if( matches[i].imgIdx == trainImgIdx )
            mask[i] = 1;
    }
}

static bool readImages( const string& queryImageName, Mat& queryImage, vector <Mat>& trainImages, vector<string>& trainImageNames ){

    queryImage = imread( queryImageName, CV_LOAD_IMAGE_GRAYSCALE);

    int readImageCount = 0;
    for( size_t i = 0; i < trainImageNames.size(); i++ ){

        Mat img = imread( trainImageNames[i], CV_LOAD_IMAGE_GRAYSCALE );
        trainImages.push_back( img );
    }

    return true;
}

static void detectKeypoints( const Mat& queryImage, vector<KeyPoint>& queryKeypoints,
                      const vector<Mat>& trainImages, vector<vector<KeyPoint> >& trainKeypoints,
                      Ptr<xfeatures2d::SURF>& featureDetector ) {

    featureDetector->detect( queryImage, queryKeypoints );
    featureDetector->detect( trainImages, trainKeypoints );
}

static void computeDescriptors( const Mat& queryImage, vector<KeyPoint>& queryKeypoints, Mat& queryDescriptors,
                         const vector<Mat>& trainImages, vector<vector<KeyPoint> >& trainKeypoints, vector<Mat>& trainDescriptors,
                         Ptr<xfeatures2d::SURF>& descriptorExtractor ) {

    descriptorExtractor->compute( queryImage, queryKeypoints, queryDescriptors );
    descriptorExtractor->compute( trainImages, trainKeypoints, trainDescriptors );

}

static void matchDescriptors( const Mat& queryDescriptors, const vector<Mat>& trainDescriptors,
                       vector<DMatch>& matches, FlannBasedMatcher& descriptorMatcher, const vector<Mat>& trainImages, const vector<string>& trainImagesNames ) {

	std::stringstream s;

    descriptorMatcher.add( trainDescriptors );
    descriptorMatcher.train();

    descriptorMatcher.match( queryDescriptors, matches );

    CV_Assert( queryDescriptors.rows == (int)matches.size() || matches.empty() );

    for( int i = 0; i < trainDescriptors.size(); i++){

        std::vector< std::vector< DMatch> > matches2;

        std::vector< DMatch > good_matches;

        descriptorMatcher.knnMatch( queryDescriptors, trainDescriptors[i], matches2, 2);
        CV_Assert( queryDescriptors.rows == (int)matches2.size() || matches2.empty() );

        for (int j = 0; j < matches2.size(); ++j){
            const float ratio = 0.8; // As in Lowe's paper; can be tuned
            if (matches2[j][0].distance < ratio * matches2[j][1].distance){
                good_matches.push_back(matches2[j][0]);
            }

        }

        currentMatchSize = good_matches.size();

	    if(currentMatchSize >= 35){

			//img_2_final = img_2;
			//keypoints_1_final = keypoints_1;
			//keypoints_2_final = keypoints_2;
			good_matches_final = good_matches;

			bestMatchSize = currentMatchSize;
			similarImageName = trainImagesNames[i];
	    }

    }

    if(bestMatchSize > 0){
	    isFoundSimilar = true;

	}

  	s << "{\"isFoundSimilar\" : " << isFoundSimilar << ",\"imageName\" : \"" << similarImageName << "\",\"goodMatches\" : " << bestMatchSize << "}";

	cout << s.str() << endl;

    
}

int main(int argc, char** argv){

	clock_t tStart = clock();

    string argvStrings[argc];
	argvStrings[0] = string(argv[0]); //program name
	argvStrings[1] = string(argv[1]); //image that is to be checked 

	vector<string> trainImagesNames;

	for(int i = 2; i < argc; i++){
	    argvStrings[i] = string(argv[i]);
	    trainImagesNames.push_back(argvStrings[i]);
	}

    int minHessian = 400;
    Ptr<xfeatures2d::SURF> featureDetector;
    Ptr<xfeatures2d::SURF> descriptorExtractor;
    FlannBasedMatcher descriptorMatcher;

    featureDetector = xfeatures2d::SURF::create( minHessian );
    descriptorExtractor = xfeatures2d::SURF::create( );

    bool isCreated = !( featureDetector.empty() || descriptorExtractor.empty());
    if( !isCreated ){
        cout << "Can not create feature detector or descriptor extractor or descriptor matcher of given types." << endl << ">" << endl;
        return -1;
    }

    Mat queryImage;
    vector<Mat> trainImages;
    if( !readImages( argvStrings[1], queryImage, trainImages, trainImagesNames ) ){

        cout << "Cant read images " << endl;
        return -1;
    }

    vector<KeyPoint> queryKeypoints;
    vector<vector<KeyPoint> > trainKeypoints;
    detectKeypoints( queryImage, queryKeypoints, trainImages, trainKeypoints, featureDetector );

    Mat queryDescriptors;
    vector<Mat> trainDescriptors;
    computeDescriptors( queryImage, queryKeypoints, queryDescriptors,
                        trainImages, trainKeypoints, trainDescriptors,
                        descriptorExtractor );

    vector<DMatch> matches;
    matchDescriptors( queryDescriptors, trainDescriptors, matches, descriptorMatcher, trainImages, trainImagesNames );

    //cout << "Time taken: " << (double)(clock() - tStart)/CLOCKS_PER_SEC << endl;

    return 0;
}
