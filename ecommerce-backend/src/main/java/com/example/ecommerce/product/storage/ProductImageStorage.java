package com.example.ecommerce.product.storage;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProductImageStorage {
    private final Path root;
    public ProductImageStorage(@Value("${app.catalog.image-directory:./uploads/product-images}") String directory) {
        root=Path.of(directory).toAbsolutePath().normalize();
    }
    public String resourceLocation() { return root.toUri().toString() + (root.toUri().toString().endsWith("/")?"":"/"); }
    public record PreparedImage(byte[] bytes, String extension) {}
    public PreparedImage validate(MultipartFile file) {
        if(file.isEmpty() || file.getSize()>5L*1024*1024) throw com.example.ecommerce.common.ApiErrors.bad("Each image must contain 1 byte to 5 MiB");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            if(input==null) throw com.example.ecommerce.common.ApiErrors.bad("Invalid image");
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw com.example.ecommerce.common.ApiErrors.bad("Only PNG and JPEG images are supported");
            var reader=readers.next();
            try {
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("png","jpeg","jpg").contains(format)) throw com.example.ecommerce.common.ApiErrors.bad("Only PNG and JPEG images are supported");
                reader.setInput(input,true,true);
                int width=reader.getWidth(0), height=reader.getHeight(0);
                if(width<1 || height<1 || (long)width*height>20_000_000) throw com.example.ecommerce.common.ApiErrors.bad("Image dimensions exceed 20 megapixels");
                var decoded=reader.read(0); var output=new ByteArrayOutputStream();
                String extension=format.equals("png")?"png":"jpg";
                if(!ImageIO.write(decoded,extension,output)) throw com.example.ecommerce.common.ApiErrors.bad("Invalid image");
                decoded.flush();
                if(output.size()>5*1024*1024) throw com.example.ecommerce.common.ApiErrors.bad("Re-encoded image exceeds 5 MiB");
                return new PreparedImage(output.toByteArray(),extension);
            } finally { reader.dispose(); }
        } catch(IOException | IllegalArgumentException e) { throw com.example.ecommerce.common.ApiErrors.bad("Invalid or unreadable image"); }
    }
    public String save(PreparedImage image) throws IOException {
        Files.createDirectories(root);
        String name=UUID.randomUUID()+"."+image.extension();
        Path target=root.resolve(name);
        try { Files.write(target,image.bytes(),StandardOpenOption.CREATE_NEW); }
        catch(IOException e) { Files.deleteIfExists(target); throw e; }
        return name;
    }
    public void delete(String name) {
        try { Files.deleteIfExists(root.resolve(name)); }
        catch(IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Failed to clean up rolled-back product image {}",name,e); }
    }
}
