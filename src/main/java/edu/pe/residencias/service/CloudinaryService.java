package edu.pe.residencias.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private Cloudinary cloudinary;

    @Value("${cloudinary.url:}")
    private String cloudinaryUrlProp;

    public CloudinaryService() {
        // Cloudinary will be initialized in init() to allow @Value injection
        this.cloudinary = null;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        String cloudinaryUrl = System.getenv("CLOUDINARY_URL");
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            cloudinaryUrl = cloudinaryUrlProp;
        }
        if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            this.cloudinary = new Cloudinary(cloudinaryUrl);
            return;
        }
        // Try individual env vars (optional)
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");
        if (cloudName != null && apiKey != null && apiSecret != null) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
            ));
            return;
        }
        throw new IllegalStateException("Cloudinary credentials not configured. Set CLOUDINARY_URL or cloudinary.url property.");
    }

    public Map uploadImage(MultipartFile file, String folder) throws Exception {
        Map params = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "image",
            "use_filename", true,
            // Let Cloudinary generate a unique filename to avoid collisions
            "unique_filename", true,
            "overwrite", false
        );
        return cloudinary.uploader().upload(file.getBytes(), params);
    }

    public Map uploadRaw(MultipartFile file, String folder) throws Exception {
        Map params = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "raw",
            "use_filename", true,
            // Let Cloudinary generate a unique filename to avoid collisions
            "unique_filename", true,
            "overwrite", false
        );
        return cloudinary.uploader().upload(file.getBytes(), params);
    }

    public Map destroy(String publicId, String resourceType) throws Exception {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
    }

    public String generateTransformedUrl(String publicId, int width, int height) {
        try {
            return cloudinary.url().transformation(new com.cloudinary.Transformation().width(width).height(height).crop("fill")).generate(publicId);
        } catch (Exception ex) {
            return null;
        }
    }
}
