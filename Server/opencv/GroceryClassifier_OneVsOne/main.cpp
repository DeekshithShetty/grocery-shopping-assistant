#include "opencv2/core/core.hpp"
#include "opencv2/opencv.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/xfeatures2d.hpp"
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/ml.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/xfeatures2d.hpp>
#include "opencv2/ml.hpp"

#include <map>
#include <omp.h>
#include <utility>
#include <iostream>
#include <fstream>
#include <sstream>
#include <time.h> 

#define INPUT_IMAGE_LABEL (-1)

#define TOMATO_LABEL (0)
#define LEMON_LABEL (1)
#define BEANS_LABEL (2)
#define BANANA_LABEL (3)


typedef std::pair<int, cv::Mat> DatabaseElement;
typedef std::vector<DatabaseElement> DatabaseType;

// naming convention: [fruit_name]_[#].jpg
bool loadImages(const std::string& path, DatabaseType& outDatabase){

	cv::Mat tomatoImage, lemonImage, beansImage, bananaImage;
	int counter = 1;
	//std::cout << "----------- Loading train images from: " << path << std::endl;
	do {
		tomatoImage = cv::imread(path + "tomato_" + std::to_string(counter) + ".jpg");
		lemonImage = cv::imread(path + "lemon_" + std::to_string(counter) + ".jpg");
		beansImage = cv::imread(path + "beans_" + std::to_string(counter) + ".jpg");
		bananaImage = cv::imread(path + "banana_" + std::to_string(counter) + ".jpg");

		if (!tomatoImage.empty()){
			outDatabase.push_back(std::make_pair(TOMATO_LABEL, tomatoImage));
		}
		if (!lemonImage.empty()){
			outDatabase.push_back(std::make_pair(LEMON_LABEL, lemonImage));
		}
		if (!beansImage.empty()){
			outDatabase.push_back(std::make_pair(BEANS_LABEL, beansImage));
		}
		if (!bananaImage.empty()){
			outDatabase.push_back(std::make_pair(BANANA_LABEL, bananaImage));
		}
		counter++;
	} while (!(tomatoImage.empty() && lemonImage.empty() && beansImage.empty() && bananaImage.empty()));

	std::cout << "----------- Number of train images loaded: " << outDatabase.size() << std::endl;

	return !outDatabase.empty();
}

bool createVocabulary(const DatabaseType& trainingDb, cv::Mat& outVocabulary){

	CV_Assert(!trainingDb.empty());
	cv::Ptr<cv::FeatureDetector> detector = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorExtractor> extractor = cv::xfeatures2d::SURF::create();
	cv::Mat trainingDescriptors(1, extractor->descriptorSize(), extractor->descriptorType());
	outVocabulary.create(0, 1, CV_32FC1);
	cv::BOWKMeansTrainer bowtrainer(1000);

	for (auto& it : trainingDb){
		std::vector<cv::KeyPoint> keypoints;
		detector->detect(it.second, keypoints);

		if (!keypoints.empty()){
			cv::Mat descriptors;
			extractor->compute(it.second, keypoints, descriptors);

			if (!descriptors.empty()){
				//std::cout << "----------- Adding " << descriptors.rows << " training descriptors." << std::endl;
				trainingDescriptors.push_back(descriptors);
				bowtrainer.add(descriptors);
			}else{
				std::cout << "----------- No descriptors found." << std::endl;
			}
		}else{
			std::cout << "----------- No keypoints found." << std::endl;
		}
	}

	if (trainingDescriptors.empty()){
		std::cout << "----------- Training descriptors are empty." << std::endl;
		return false;
	}
	//std::cout << "----------- trainingDescriptors.size() : " << trainingDescriptors.size() <<  std::endl;

	outVocabulary = bowtrainer.cluster();
	return true;
}

bool scourTrainingSet(const DatabaseType& trainingDb, const cv::Mat& vocabulary, cv::Mat& outSamples, cv::Mat& outLabels){

	CV_Assert(!trainingDb.empty());
	CV_Assert(!vocabulary.empty());

	cv::Ptr<cv::FeatureDetector> detector = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorExtractor> extractor = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorMatcher> matcher = cv::DescriptorMatcher::create("BruteForce");

	cv::BOWImgDescriptorExtractor bowide(extractor, matcher);
	bowide.setVocabulary(vocabulary);

	cv::Mat samples;
	outSamples.create(0, 1, CV_32FC1);
	outLabels.create(0, 1, CV_32SC1);

	//#pragma omp parallel for schedule(dynamic,3)
	for (auto& it : trainingDb){

		std::vector<cv::KeyPoint> keypoints;
		detector->detect(it.second, keypoints);

		if (!keypoints.empty()){
			cv::Mat descriptors;
			bowide.compute(it.second, keypoints, descriptors);

			//#pragma omp critical
			//{
				if (!descriptors.empty()){
					if (samples.empty()){
						samples.create(0, descriptors.cols, descriptors.type());
					}
					// Copy class samples and labels
					//std::cout << "----------- Adding " << descriptors.rows << " positive sample." << std::endl;
					samples.push_back(descriptors);

					cv::Mat classLabels;

					if (it.first == TOMATO_LABEL){
						classLabels = cv::Mat::zeros(descriptors.rows, 1, CV_32SC1);
					}else if (it.first == LEMON_LABEL){
						classLabels = cv::Mat::ones(descriptors.rows, 1, CV_32SC1);
					}else if (it.first == BEANS_LABEL){
						classLabels = cv::Mat(descriptors.rows,1, CV_32SC1, double(2));
					}else if (it.first == BANANA_LABEL){
						classLabels = cv::Mat(descriptors.rows,1, CV_32SC1, double(3));
					}

					outLabels.push_back(classLabels);
				}else{
					std::cout << "----------- No descriptors found." << std::endl;
				}
			//}
		}else{
			std::cout << "----------- No keypoints found." << std::endl;
		}
	}

	if (samples.empty() || outLabels.empty()){
		std::cout << "----------- Samples are empty." << std::endl;
		return false;
	}
	samples.convertTo(outSamples, CV_32FC1);

	return true;
}

bool trainSVM(const cv::Mat& samples, const cv::Mat& labels, cv::Ptr<cv::ml::SVM>& outSVM) {
	CV_Assert(!samples.empty() && samples.type() == CV_32FC1);
	CV_Assert(!labels.empty() && labels.type() == CV_32SC1);

	outSVM = cv::ml::SVM::create();

	return outSVM->train(samples, cv::ml::ROW_SAMPLE, labels);
}

bool testSVM(const DatabaseType& testingDb, const cv::Mat& vocabulary, const cv::Ptr<cv::ml::SVM>& SVM){

	CV_Assert(!testingDb.empty());
	CV_Assert(!vocabulary.empty());

	cv::Ptr<cv::FeatureDetector> detector = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorExtractor> extractor = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorMatcher> matcher = cv::DescriptorMatcher::create("BruteForce");

	cv::BOWImgDescriptorExtractor bowide(extractor, matcher);
	bowide.setVocabulary(vocabulary);

	for (auto& it : testingDb){

		std::vector<cv::KeyPoint> keypoints;
		detector->detect(it.second, keypoints);

		if (keypoints.empty()) continue;

		// Responses to the vocabulary
		cv::Mat imgDescriptor;
		bowide.compute(it.second, keypoints, imgDescriptor);

		if (imgDescriptor.empty()) continue;

		cv::Mat results;
		std::string predicted_label;
		float res = SVM->predict(imgDescriptor);

		if( res == TOMATO_LABEL ){
			predicted_label = "Tomato";
		}else if( res == LEMON_LABEL ){
			predicted_label = "Lemon";
		}else if( res == BEANS_LABEL ){
			predicted_label = "Beans";
		}else if( res == BANANA_LABEL ){
			predicted_label = "Banana";
		}
		
		std::cout << "******** Result of prediction: " << predicted_label << " *********** "<< std::endl;

	}

	return true;
}


int main(int argc, char** argv){

	std::cout << "----- 1. Loading images ------- " << std::endl;

	const std::string& trainingPath = "./train_data/";

	DatabaseType trainingDb, testingDb;

	if (!loadImages(trainingPath, trainingDb)) {	return -1;	}

	cv::Mat inputImage = cv::imread(argv[1]);
	if (!inputImage.empty()){
		testingDb.push_back(std::make_pair(INPUT_IMAGE_LABEL, inputImage));
	}
	//std::cout << "----------- Loaded input image" << std::endl;

	std::cout << std::endl;
	// -----------------------------------------------------------------------------------

	std::cout << "----- 2. Vocabulary for BOW -------" << std::endl;

	cv::Mat vocabulary;

	//reading vocabulary from vocabulary.yml
	cv::FileStorage fs_vocabulary_read("./YAML/vocabulary.yml", cv::FileStorage::READ);
	if (fs_vocabulary_read.isOpened()){

	  fs_vocabulary_read["vocabulary"] >> vocabulary;
	  fs_vocabulary_read.releaseAndGetString();
	  std::cout << "----------- Read vocabulary from /YAML/vocabulary.yml" << std::endl;
	}else{

		if (!createVocabulary(trainingDb, vocabulary)) {	return -1;	}
		//persisting vocabulary to vocabulary.yml
		cv::FileStorage fs_vocabulary_write("./YAML/vocabulary.yml", cv::FileStorage::WRITE);
	    fs_vocabulary_write << "vocabulary" << vocabulary;
	    fs_vocabulary_write.release();

	    std::cout << "----------- Vocabulary written to /YAML/vocabulary.yml" << std::endl;
	}
	
	std::cout << std::endl;
	// ------------------------------------------------------------------------------------

	std::cout << "----- 3. Scour the training set for our histograms ----- " << std::endl;

	cv::Mat samples_32f;
	cv::Mat labels;

	//reading histograms from histograms.yml and reading labels from labels.yml
	cv::FileStorage fs_histograms_read("./YAML/histograms.yml", cv::FileStorage::READ);
	cv::FileStorage fs_labels_read("./YAML/labels.yml", cv::FileStorage::READ);

	if (fs_histograms_read.isOpened() && fs_labels_read.isOpened()){

	  fs_histograms_read["histograms"] >> samples_32f;
	  fs_histograms_read.releaseAndGetString();
	  std::cout << "----------- Read histograms from histograms.yml" << std::endl;

	  fs_labels_read["labels"] >> labels;
	  fs_labels_read.releaseAndGetString();
	  std::cout << "----------- Read labels from labels.yml" << std::endl;
	}else{

		if (!scourTrainingSet(trainingDb, vocabulary, samples_32f, labels)) {	return -1;	}

		//persisting samples_32f to histograms.yml
		cv::FileStorage fs_histograms_write("./YAML/histograms.yml", cv::FileStorage::WRITE);
	    fs_histograms_write << "histograms" << samples_32f;
	    fs_histograms_write.release();
	
	    //persisting samples_32f to histograms.yml
		cv::FileStorage fs_labels_write("./YAML/labels.yml", cv::FileStorage::WRITE);
	    fs_labels_write << "labels" << labels;
	    fs_labels_write.release();

	    std::cout << "----------- Histograms written to /YAML/histograms.yml" << std::endl;
	    std::cout << "----------- Labels written to /YAML/labels.yml" << std::endl;
	}
	
	std::cout << std::endl;
	// --------------------------------------------------------------------------------------

	std::cout << "----- 4. Training SVM ----- " << std::endl;

	cv::Ptr<cv::ml::SVM> SVM;
	if (!trainSVM(samples_32f, labels, SVM)){	return -1;	}

	std::cout << std::endl;
	// --------------------------------------------------------------------------------------

	std::cout << "----- 5. Testing SVM ----- " << std::endl;

	if (!testSVM(testingDb, vocabulary, SVM)){	return -1;	}

	std::cout << std::endl;
	// -------------------------------------------



	return 1;
}
