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
#define APPLE_LABEL (1)
#define MANGO_LABEL (2)
#define BANANA_LABEL (3)


typedef std::pair<int, cv::Mat> DatabaseElement;
typedef std::vector<DatabaseElement> DatabaseType;

std::map<int,cv::Mat> classes_training_data;
std::vector<int> classes_names;
std::map<int,std::map<int,int> > confusion_matrix; // confusionMatrix[classA][classB] = number_of_times_A_voted_for_B;
std::map<int,cv::Ptr<cv::ml::SVM> > classes_classifiers;

// naming convention: [fruit_name]_[#].jpg
bool loadImages(const std::string& path, DatabaseType& outDatabase){

	cv::Mat tomatoImage, appleImage, mangoImage, bananaImage;
	int counter = 1;
	//std::cout << "----------- Loading train images from: " << path << std::endl;
	do {
		tomatoImage = cv::imread(path + "tomato_" + std::to_string(counter) + ".jpg");
		appleImage = cv::imread(path + "apple_" + std::to_string(counter) + ".jpg");
		mangoImage = cv::imread(path + "mango_" + std::to_string(counter) + ".jpg");
		bananaImage = cv::imread(path + "banana_" + std::to_string(counter) + ".jpg");

		if (!tomatoImage.empty()){
			outDatabase.push_back(std::make_pair(TOMATO_LABEL, tomatoImage));
		}
		if (!appleImage.empty()){
			outDatabase.push_back(std::make_pair(APPLE_LABEL, appleImage));
		}
		if (!mangoImage.empty()){
			outDatabase.push_back(std::make_pair(MANGO_LABEL, mangoImage));
		}
		if (!bananaImage.empty()){
			outDatabase.push_back(std::make_pair(BANANA_LABEL, bananaImage));
		}
		counter++;
	} while (!(tomatoImage.empty() && appleImage.empty() && mangoImage.empty() && bananaImage.empty()));

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

bool scourTrainingSet(const DatabaseType& trainingDb, const cv::Mat& vocabulary){

	CV_Assert(!trainingDb.empty());
	CV_Assert(!vocabulary.empty());

	cv::Ptr<cv::FeatureDetector> detector = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorExtractor> extractor = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorMatcher> matcher = cv::DescriptorMatcher::create("BruteForce");

	cv::BOWImgDescriptorExtractor bowide(extractor, matcher);
	bowide.setVocabulary(vocabulary);

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

					if(classes_training_data.count(it.first) == 0) { //not yet created...
		             	classes_training_data[it.first].create(0,descriptors.cols,descriptors.type());
		             	classes_names.push_back(it.first);
		          	}
		          	classes_training_data[it.first].push_back(descriptors);

				}else{
					std::cout << "----------- No descriptors found." << std::endl;
				}
			//}
		}else{
			std::cout << "----------- No keypoints found." << std::endl;
		}
	}
	//samples.convertTo(outSamples, CV_32FC1);

	return true;
}

bool trainSVM( ) {

	for (size_t i = 0; i < classes_names.size(); i++) {
       int class_ = classes_names[i];
       std::cout << " training class: " << class_ << ".." << std::endl;
             
       cv::Mat samples(0,0,CV_32FC1);
       cv::Mat labels(0, 1, CV_32SC1);

       //reading histogram_ from histogram_.yml
       std::string histogram_path = "./YAML/histogram_" + std::to_string(class_) + ".yml";
       std::string labels_path = "./YAML/labels_" + std::to_string(class_) + ".yml";

		cv::FileStorage fs_histogram_read( histogram_path, cv::FileStorage::READ);
		cv::FileStorage fs_labels_read( labels_path, cv::FileStorage::READ);

		if (fs_histogram_read.isOpened() && fs_labels_read.isOpened()){

		  fs_histogram_read["histogram"] >> samples;
		  fs_histogram_read.releaseAndGetString();
		  std::cout << "----------- Read histogram from " << histogram_path << std::endl;

		  fs_labels_read["labels"] >> labels;
	  	  fs_labels_read.releaseAndGetString();
	  	  std::cout << "----------- Read labels from " << labels_path << std::endl;

		}else{

			//copy class samples and label
	       std::cout << "adding " << classes_training_data[class_].rows << " positive" << std::endl;
	       samples.push_back(classes_training_data[class_]);
	       cv::Mat class_label = cv::Mat::ones(classes_training_data[class_].rows, 1, CV_32SC1);
	       labels.push_back(class_label);
	             
	       //copy rest samples and label
	       for (std::map<int,cv::Mat>::iterator it1 = classes_training_data.begin(); it1 != classes_training_data.end(); ++it1) {
	          int not_class_ = (*it1).first;
	          if(not_class_ == class_) continue; //skip class itself
	          samples.push_back(classes_training_data[not_class_]);
	          class_label = cv::Mat::zeros(classes_training_data[not_class_].rows, 1, CV_32SC1);
	          labels.push_back(class_label);
	       }


			//persisting samples to histogram_i.yml
			cv::FileStorage fs_histogram_write( histogram_path, cv::FileStorage::WRITE);
		    fs_histogram_write << "histogram" << samples;
		    fs_histogram_write.release();

		    //persisting labels to labels_i.yml
			cv::FileStorage fs_labels_write( labels_path, cv::FileStorage::WRITE);
	    	fs_labels_write << "labels" << labels;
	    	fs_labels_write.release();

		    std::cout << "----------- Histograms written to " << histogram_path << std::endl;
	    	std::cout << "----------- Labels written to " << labels_path << std::endl;
		}
        
       std::cout << "Train.." << std::endl;
       if(samples.rows == 0) continue; //phantom class?!
       cv::Mat samples_32f; 
	   samples.convertTo(samples_32f, CV_32F);
       cv::Ptr<cv::ml::SVM> classifier = cv::ml::SVM::create();
       classifier->train(samples_32f, cv::ml::ROW_SAMPLE, labels);
     
       //classes_classifiers[class_].push_back(classifier);
       classes_classifiers[class_] = classifier;
       //do something with the classifier, like saving it to file
    }

	return true;
}

bool testSVM(const DatabaseType& testingDb, const cv::Mat& vocabulary){

	CV_Assert(!testingDb.empty());
	CV_Assert(!vocabulary.empty());

	cv::Ptr<cv::FeatureDetector> detector = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorExtractor> extractor = cv::xfeatures2d::SURF::create();
	cv::Ptr<cv::DescriptorMatcher> matcher = cv::DescriptorMatcher::create("BruteForce");

	cv::BOWImgDescriptorExtractor bowide(extractor, matcher);
	bowide.setVocabulary(vocabulary);


	for (auto& it : testingDb){

       cv::Mat img = it.second;
        
       std::vector<cv::KeyPoint> keypoints;
       detector->detect(img,keypoints);

       if (keypoints.empty()) continue;

       cv::Mat response_hist;
       bowide.compute(img, keypoints, response_hist);

       if (response_hist.empty()) continue;
     
       float minf = FLT_MAX;
       int minclass;
       for (std::map<int,cv::Ptr<cv::ml::SVM> >::iterator it2 = classes_classifiers.begin(); it2 != classes_classifiers.end(); ++it2) {
          float res = (*it2).second->predict(response_hist, cv::noArray(),cv::ml::StatModel::Flags::RAW_OUTPUT);
          std::cout << "res : " << res << std::endl;

          if (res < minf) {
             minf = res;
             minclass = (*it2).first;
             std::cout << "minclass : " << minclass << std::endl;
          }
       }

       // confusionMatrix[classA][classB] = number_of_times_A_voted_for_B;
       //std::map<int,std::map<int,int> > confusion_matrix; 
       //confusion_matrix[minclass][classes[i]]++;

       std::string predicted_label;

       if( minclass == TOMATO_LABEL ){
       		predicted_label = "Tomato";
       }else if( minclass == APPLE_LABEL ){
			predicted_label = "Apple";
	   }else if( minclass == MANGO_LABEL ){
			predicted_label = "Mango";
	   }else if( minclass == BANANA_LABEL ){
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

	if (!scourTrainingSet(trainingDb, vocabulary)) {	return -1;	}
	
	std::cout << std::endl;
	// --------------------------------------------------------------------------------------

	std::cout << "----- 4. Training SVM ----- " << std::endl;

	cv::Ptr<cv::ml::SVM> SVM;
	if (!trainSVM()){	return -1;	}

	std::cout << std::endl;
	// --------------------------------------------------------------------------------------

	std::cout << "----- 5. Testing SVM ----- " << std::endl;

	if (!testSVM(testingDb, vocabulary)){	return -1;	}

	std::cout << std::endl;
	// -------------------------------------------



	return 1;
}
