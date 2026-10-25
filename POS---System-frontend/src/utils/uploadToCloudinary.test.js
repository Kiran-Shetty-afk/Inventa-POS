import { afterEach, describe, expect, it, vi } from "vitest";
import { uploadToCloudinary } from "./uploadToCloudinary";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("uploadToCloudinary", () => {
  it("rejects unsupported image types before making a request", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const file = new File(["payload"], "document.svg", { type: "image/svg+xml" });

    await expect(uploadToCloudinary(file)).rejects.toThrow("Upload a JPEG");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects images larger than the upload limit", async () => {
    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], "large.png", {
      type: "image/png",
    });

    await expect(uploadToCloudinary(file)).rejects.toThrow("5 MB");
  });

  it("returns only Cloudinary's secure URL", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ secure_url: "https://res.cloudinary.com/example/image.png", url: "http://insecure" }),
    }));
    const file = new File(["image"], "image.png", { type: "image/png" });

    await expect(uploadToCloudinary(file)).resolves.toBe(
      "https://res.cloudinary.com/example/image.png"
    );
  });

  it("reports HTTP upload failures without returning response data", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ error: { message: "provider details" } }),
    }));
    const file = new File(["image"], "image.png", { type: "image/png" });

    await expect(uploadToCloudinary(file)).rejects.toThrow("Image upload failed");
  });
});
