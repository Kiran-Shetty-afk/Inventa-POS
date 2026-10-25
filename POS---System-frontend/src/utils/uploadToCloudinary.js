const CLOUDINARY_CLOUD_NAME = "dxoqwusir";
const CLOUDINARY_UPLOAD_PRESET = "ml_default";
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/gif",
]);

export const uploadToCloudinary = async (file) => {
  if (!file) {
    throw new Error("Choose an image to upload.");
  }
  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    throw new Error("Upload a JPEG, PNG, WebP, or GIF image.");
  }
  if (file.size <= 0 || file.size > MAX_IMAGE_BYTES) {
    throw new Error("Image size must be between 1 byte and 5 MB.");
  }

  const data = new FormData();
  data.append("file", file);
  data.append("upload_preset", CLOUDINARY_UPLOAD_PRESET);

  const response = await fetch(
    `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`,
    { method: "POST", body: data }
  );
  let result;
  try {
    result = await response.json();
  } catch {
    throw new Error("Image upload returned an invalid response.");
  }
  if (!response.ok) {
    throw new Error("Image upload failed. Please try again.");
  }

  const secureUrl = result?.secure_url;
  if (typeof secureUrl !== "string" || !secureUrl.startsWith("https://")) {
    throw new Error("Image upload did not return a secure image URL.");
  }
  return secureUrl;
};
