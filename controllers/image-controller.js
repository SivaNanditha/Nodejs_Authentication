const Image = require("../models/image");
const { uploadToCloudinary } = require("../helpers/cloudinary-Helper");
const fs = require("fs");

const cloudinary = require("../config/cloudinary");
const uploadImageController = async (req, res) => {
  try {
    //check file is sent input or not
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: "Please upload an Image",
      });
    }
    //upload Image to cloudinary
    const { url, publicId } = await uploadToCloudinary(req.file.path);

    //store the image url and public id along with uploaded user id in db

    const newlyUploadedImage = new Image({
      url,
      publicId,
      uploadedBy: req.userInfo.userId,
    });

    await newlyUploadedImage.save();

    fs.unlinkSync(req.file.path); //deletes the image from our system(vs code)

    res.status(201).json({
      success: true,
      message: "Image uploaded successfully",
      image: newlyUploadedImage,
    });
  } catch (err) {
    console.log(err);
    res.status(500).json({
      success: false,
      message: "Error uploading image",
    });
  }
};

const fetchImagesController = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1; //it will give the current page on which user will be.
    const limit = parseInt(req.query.limit) || 2; //howmany images we want to show in single page
    const skip = (page - 1) * limit; //previous page images will be skipped

    const sortBy = req.query.sortBy || "createdAt"; //sort by which field
    const sortOrder = req.query.sortOrder === "asc" ? 1 : -1; //sort order asc or desc
    const totalImages = await Image.countDocuments();
    const totalPages = Math.ceil(totalImages / limit);
    const sortObj = {};
    sortObj[sortBy] = sortOrder;
    const images = await Image.find().sort(sortObj).skip(skip).limit(limit);

    if (images) {
      res.status(200).json({
        success: true,
        currentPage: page,
        totalPages: totalPages,
        totalImages: totalImages,
        data: images,
      });
    }
  } catch (e) {
    console.log(err);
    res.status(500).json({
      success: false,
      message: "Error uploading image",
    });
  }
};

const deleteImageController = async (req, res) => {
  try {
    const getCurrentIdOfImageToBeDeleted = req.params.id;
    const userId = req.userInfo.userId;

    const image = await Image.findById(getCurrentIdOfImageToBeDeleted);
    if (!image) {
      return res
        .status(404)
        .json({ success: false, message: "Image not found" });
    }

    //check if image is uploaded by current user who is trying to delete this image
    if (image.uploadedBy.toString() !== userId) {
      return res.status(403).json({
        success: false,
        message: "You are not authorized to delete this image",
      });
    }
    //delete image frst from cloudinary
    await cloudinary.uploader.destroy(image.publicId);
    //delete image from database
    await Image.findByIdAndDelete(getCurrentIdOfImageToBeDeleted);
    res.status(200).json({
      success: true,
      message: "Image deleted successfully",
    });
  } catch (e) {
    console.log(err);
    res.status(500).json({
      success: false,
      message: "Something went wrong! Please try again later",
    });
  }
};
module.exports = {
  uploadImageController,
  fetchImagesController,
  deleteImageController,
};
