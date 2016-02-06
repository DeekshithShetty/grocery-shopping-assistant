#include "opencv2/core/core.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/xfeatures2d.hpp"

#include <iostream>
#include <fstream>
#include <sstream>
#include <time.h> 

//#include "opencv2/contrib/contrib.hpp"


using namespace cv;
using namespace std;

const string defaultDetectorType = "SURF";
const string defaultDescriptorType = "SURF";
const string defaultMatcherType = "FlannBased";
const string defaultQueryImageName = "./query.jpg";
const string defaultFileWithTrainImages = "./train/trainImages.txt";
const string defaultDirToSaveResImages = "./results";

static void printPrompt( const string& applName )
{
    cout << "/*\n"
         << " * This is a sample on matching descriptors detected on one image to descriptors detected in image set.\n"
         << " * So we have one query image and several train images. For each keypoint descriptor of query image\n"
         << " * the one nearest train descriptor is found the entire collection of train images. To visualize the result\n"
         << " * of matching we save images, each of which combines query and train image with matches between them (if they exist).\n"
         << " * Match is drawn as line between corresponding points. Count of all matches is equel to count of\n"
         << " * query keypoints, so we have the same count of lines in all set of result images (but not for each result\n"
         << " * (train) image).\n"
         << " */\n" << endl;

    cout << endl << "Format:\n" << endl;
    cout << "./" << applName << " [detectorType] [descriptorType] [matcherType] [queryImage] [fileWithTrainImages] [dirToSaveResImages]" << endl;
    cout << endl;

    cout << "\nExample:" << endl
         << "./" << applName << " " << defaultDetectorType << " " << defaultDescriptorType << " " << defaultMatcherType << " "
         << defaultQueryImageName << " " << defaultFileWithTrainImages << " " << defaultDirToSaveResImages << endl;
}

static void maskMatchesByTrainImgIdx( const vector<DMatch>& matches, int trainImgIdx, vector<char>& mask )
{
    mask.resize( matches.size() );
    fill( mask.begin(), mask.end(), 0 );
    for( size_t i = 0; i < matches.size(); i++ )
    {
        if( matches[i].imgIdx == trainImgIdx )
            mask[i] = 1;
    }
}

static void readTrainFilenames( const string& filename, string& dirName, vector<string>& trainFilenames )
{
    trainFilenames.clear();

    ifstream file( filename.c_str() );
    if ( !file.is_open() )
        return;

    size_t pos = filename.rfind('\\');
    char dlmtr = '\\';
    if (pos == String::npos)
    {
        pos = filename.rfind('/');
        dlmtr = '/';
    }
    dirName = pos == string::npos ? "" : filename.substr(0, pos) + dlmtr;

    while( !file.eof() )
    {
        string str; getline( file, str );
        if( str.empty() ) break;
        trainFilenames.push_back(str);
    }
    file.close();
}

static bool readImages( const string& queryImageName, const string& trainFilename,
                 Mat& queryImage, vector <Mat>& trainImages, vector<string>& trainImageNames )
{
    cout << "< Reading the images..." << endl;
    queryImage = imread( queryImageName, CV_LOAD_IMAGE_GRAYSCALE);
    if( queryImage.empty() )
    {
        cout << "Query image can not be read." << endl << ">" << endl;
        return false;
    }
    string trainDirName;
    readTrainFilenames( trainFilename, trainDirName, trainImageNames );
    if( trainImageNames.empty() )
    {
        cout << "Train image filenames can not be read." << endl << ">" << endl;
        return false;
    }
    int readImageCount = 0;
    for( size_t i = 0; i < trainImageNames.size(); i++ )
    {
        string filename = trainDirName + trainImageNames[i];
        Mat img = imread( filename, CV_LOAD_IMAGE_GRAYSCALE );
        if( img.empty() )
            cout << "Train image " << filename << " can not be read." << endl;
        else
            readImageCount++;
        trainImages.push_back( img );
    }
    if( !readImageCount )
    {
        cout << "All train images can not be read." << endl << ">" << endl;
        return false;
    }
    else
        cout << readImageCount << " train images were read." << endl;
    cout << ">" << endl;

    return true;
}

static void detectKeypoints( const Mat& queryImage, vector<KeyPoint>& queryKeypoints,
                      const vector<Mat>& trainImages, vector<vector<KeyPoint> >& trainKeypoints,
                      Ptr<xfeatures2d::SURF>& featureDetector )
{
    cout << endl << "< Extracting keypoints from images..." << endl;
    featureDetector->detect( queryImage, queryKeypoints );
    featureDetector->detect( trainImages, trainKeypoints );
    cout << ">" << endl;
}

static void computeDescriptors( const Mat& queryImage, vector<KeyPoint>& queryKeypoints, Mat& queryDescriptors,
                         const vector<Mat>& trainImages, vector<vector<KeyPoint> >& trainKeypoints, vector<Mat>& trainDescriptors,
                         Ptr<xfeatures2d::SURF>& descriptorExtractor )
{
    cout << "< Computing descriptors for keypoints..." << endl;
    descriptorExtractor->compute( queryImage, queryKeypoints, queryDescriptors );
    descriptorExtractor->compute( trainImages, trainKeypoints, trainDescriptors );

    int totalTrainDesc = 0;
    for( vector<Mat>::const_iterator tdIter = trainDescriptors.begin(); tdIter != trainDescriptors.end(); tdIter++ )
        totalTrainDesc += tdIter->rows;

    cout << "Query descriptors count: " << queryDescriptors.rows << "; Total train descriptors count: " << totalTrainDesc << endl;
    cout << ">" << endl;
}

//static void matchDescriptors( const Mat& queryDescriptors, const vector<Mat>& trainDescriptors,
                       //vector<DMatch>& matches, FlannBasedMatcher& descriptorMatcher )
static void matchDescriptors( const Mat& queryDescriptors, const vector<Mat>& trainDescriptors,
                       vector<DMatch>& matches, FlannBasedMatcher& descriptorMatcher, const vector<Mat>& trainImages, const vector<string>& trainImagesNames )

{
    cout << "< Set train descriptors collection in the matcher and match query descriptors to them..." << endl;

    descriptorMatcher.add( trainDescriptors );
    descriptorMatcher.train();

    descriptorMatcher.match( queryDescriptors, matches );

    CV_Assert( queryDescriptors.rows == (int)matches.size() || matches.empty() );

    cout << "Number of matches: " << matches.size() << endl;
    cout << ">" << endl;

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

        cout << "currentMatchSize : " << good_matches.size() << endl;

    }

    
}

static void saveResultImages( const Mat& queryImage, const vector<KeyPoint>& queryKeypoints,
                       const vector<Mat>& trainImages, const vector<vector<KeyPoint> >& trainKeypoints,
                       const vector<DMatch>& matches, const vector<string>& trainImagesNames, const string& resultDir )
{
    cout << "< Save results..." << endl;
    Mat drawImg;
    vector<char> mask;
    for( size_t i = 0; i < trainImages.size(); i++ )
    {
        if( !trainImages[i].empty() )
        {
            maskMatchesByTrainImgIdx( matches, (int)i, mask );

            drawMatches( queryImage, queryKeypoints, trainImages[i], trainKeypoints[i],
                         matches, drawImg, Scalar(255, 0, 0), Scalar(0, 255, 255), mask );
            string filename = resultDir + "/res_" + trainImagesNames[i];
            if( !imwrite( filename, drawImg ) )
                cout << "Image " << filename << " can not be saved (may be because directory " << resultDir << " does not exist)." << endl;
        }
    }
    cout << ">" << endl;
}

int main(int argc, char** argv)
{

    clock_t tStart = clock();

    string detectorType = defaultDetectorType;
    string descriptorType = defaultDescriptorType;
    string matcherType = defaultMatcherType;
    string queryImageName = defaultQueryImageName;
    string fileWithTrainImages = defaultFileWithTrainImages;
    string dirToSaveResImages = defaultDirToSaveResImages;

    if( argc != 7 && argc != 1 )
    {
        printPrompt( argv[0] );
        return -1;
    }

    if( argc != 1 )
    {
        detectorType = argv[1]; descriptorType = argv[2]; matcherType = argv[3];
        queryImageName = argv[4]; fileWithTrainImages = argv[5];
        dirToSaveResImages = argv[6];
    }

    int minHessian = 400;
    Ptr<xfeatures2d::SURF> featureDetector;
    //Ptr<xfeatures2d::FeatureDetector> featureDetector;
    Ptr<xfeatures2d::SURF> descriptorExtractor;
    //Ptr<xfeatures2d::SURF> descriptorMatcher;
    //-- Step 3: Matching descriptor vectors using FLANN matcher
    //std::vector< std::vector< DMatch> > matches;
    FlannBasedMatcher descriptorMatcher;

    cout << "< Creating feature detector, descriptor extractor and descriptor matcher ..." << endl;
    featureDetector = xfeatures2d::SURF::create( minHessian );
    descriptorExtractor = xfeatures2d::SURF::create( );
    //descriptorMatcher = xfeatures2d::SURF::create( matcherType );
    cout << ">" << endl;

    bool isCreated = !( featureDetector.empty() || descriptorExtractor.empty());
    if( !isCreated ){
        cout << "Can not create feature detector or descriptor extractor or descriptor matcher of given types." << endl << ">" << endl;
        return -1;
    }

    Mat queryImage;
    vector<Mat> trainImages;
    vector<string> trainImagesNames;
    if( !readImages( queryImageName, fileWithTrainImages, queryImage, trainImages, trainImagesNames ) )
    {
        printPrompt( argv[0] );
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

    saveResultImages( queryImage, queryKeypoints, trainImages, trainKeypoints,
                      matches, trainImagesNames, dirToSaveResImages );

    cout << "Time taken: " << (double)(clock() - tStart)/CLOCKS_PER_SEC << endl;
    return 0;
}
