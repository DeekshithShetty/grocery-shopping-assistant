Mat enhanceImage(Mat image){
    cv::Mat lab_image;
    cv::cvtColor(image, lab_image, CV_BGR2Lab);

    // Extract the L channel
    std::vector<cv::Mat> lab_planes(3);
    cv::split(lab_image, lab_planes);  // now we have the L image in lab_planes[0]

    // apply the CLAHE algorithm to the L channel
    cv::Ptr<cv::CLAHE> clahe = cv::createCLAHE();
    clahe->setClipLimit(4);
    cv::Mat dst;
    clahe->apply(lab_planes[0], dst);

    // Merge the the color planes back into an Lab image
    dst.copyTo(lab_planes[0]);
    cv::merge(lab_planes, lab_image);

   // convert back to RGB
   cv::Mat image_clahe;
   cv::cvtColor(lab_image, image_clahe, CV_Lab2BGR);

   return image_clahe;
}

//Label pre processing
Mat labelPreprocessing(Mat image){
    Mat grey;

    adaptiveThreshold(~image, grey, 255, CV_ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 15, -7);
    
    // Apply Histogram Equalization
    //equalizeHist( image, grey );

    cv::imshow("histogram equalization", grey);


    return grey;
}

computeNMChannels(image, channels2);
//adaptiveThreshold(channels[2],channels[2], 255, CV_ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 15, -7);
int cn = (int)channels.size();
cout << "Channel size :" << cn << endl;
// Append negative channels to detect ER- (bright regions over dark background)
//for (int c = 0; c < cn-1; c++)
    //channels.push_back(255-channels[c]);

// Notice here we are only using grey channel
channels.push_back(grey);
channels.push_back(255-grey);