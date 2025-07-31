const cloudinary = require("../config/cloudinary");

const uploadToCloudinary = async (filePath) => {
  try {
    const result = await cloudinary.uploader.upload(filePath);
    //sample response

    // {
    //   "asset_id": "3515c6000a548515f1134043f9785c2f",
    //   "public_id": "gotjephlnz2jgiu20zni",
    //   "version": 1719307544,
    //   "version_id": "7d2cc533bee9ff39f7da7414b61fce7e",
    //   "signature": "d0b1009e3271a942836c25756ce3e04d205bf754",
    //   "width": 1920,
    //   "height": 1441,
    //   "format": "jpg",
    //   "resource_type": "image",
    //   "created_at": "2024-06-25T09:25:44Z",
    //   "tags": [],
    //   "pages": 1,
    //   "bytes": 896838,
    //   "type": "upload",
    //   "etag": "2a2df1d2d2c3b675521e866599273083",
    //   "placeholder": false,
    //   "url": "http://res.cloudinary.com/cld-docs/image/upload/v1719307544/gotjephlnz2jgiu20zni.jpg",
    //   "secure_url": "https://res.cloudinary.com/cld-docs/image/upload/v1719307544/gotjephlnz2jgiu20zni.jpg",
    //   "asset_folder": "",
    //   "display_name": "gotjephlnz2jgiu20zni",
    //   "original_filename": "sample",
    //   "api_key": "614335564976464"
    // }  from this response we want secure_url and public_id, public_id is later used to delete or update the images
    return {
      url: result.secure_url,
      publicId: result.public_id,
    };
  } catch (e) {
    console.log("Error while uploading to cloudinary ", e);
    throw new Error("Error while uploading to cloudinary ");
  }
};

module.exports = {
  uploadToCloudinary,
};
