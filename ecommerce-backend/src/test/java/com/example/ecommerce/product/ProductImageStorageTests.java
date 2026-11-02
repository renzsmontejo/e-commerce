package com.example.ecommerce.product;

import com.example.ecommerce.product.storage.ProductImageStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class ProductImageStorageTests {
    @TempDir Path directory;
    @Test void validatesContentsAndIgnoresClientFilename() throws Exception {
        var storage=new ProductImageStorage(directory.toString()); var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var prepared=storage.validate(new MockMultipartFile("files","../../evil.html","text/html",bytes.toByteArray()));
        String name=storage.save(prepared);
        assertTrue(name.matches("[a-f0-9-]+[.]png")); assertTrue(Files.exists(directory.resolve(name)));
        assertEquals(2,ImageIO.read(directory.resolve(name).toFile()).getWidth());
        storage.delete(name); assertFalse(Files.exists(directory.resolve(name)));
    }
    @Test void rejectsEmptyFakeAndOversizeImages() {
        var storage=new ProductImageStorage(directory.toString());
        assertThrows(ResponseStatusException.class,()->storage.validate(new MockMultipartFile("files",new byte[0])));
        assertThrows(ResponseStatusException.class,()->storage.validate(new MockMultipartFile("files","x.png","image/png","not an image".getBytes())));
        assertThrows(ResponseStatusException.class,()->storage.validate(new MockMultipartFile("files",new byte[5*1024*1024+1])));
    }
}
