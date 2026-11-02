package com.example.ecommerce.product.service;

import com.example.ecommerce.product.storage.ProductImageStorage;

import static com.example.ecommerce.product.dto.ProductDtos.*;
import com.example.ecommerce.product.model.ProductImages;
import com.example.ecommerce.product.repository.ProductsRepository;
import com.example.ecommerce.product.repository.ProductImagesRepository;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ProductImageService {
    private final ProductsRepository products;
    private final ProductImagesRepository images;
    private final ProductImageStorage storage;
    public ProductImageService(ProductsRepository products, ProductImagesRepository images, ProductImageStorage storage) {
        this.products=products; this.images=images; this.storage=storage;
    }
    @Transactional
    public List<ImageResponse> upload(Long id, List<MultipartFile> files) {
        var product=products.findForUpdate(id).orElseThrow(()->com.example.ecommerce.common.ApiErrors.missing("Product"));
        if(files.isEmpty() || files.size()>10) throw com.example.ecommerce.common.ApiErrors.bad("Upload between 1 and 10 images");
        // Validate every file before persisting any of them.
        var prepared=files.stream().map(storage::validate).toList();
        var written=new ArrayList<String>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status!=STATUS_COMMITTED) written.forEach(storage::delete);
            }
        });
        int order=product.getImages().stream().map(ProductImages::getSortOrder).filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(-1)+1;
        boolean primary=product.getImages().stream().noneMatch(ProductImages::isPrimary);
        var saved=new ArrayList<ProductImages>();
        try {
            for(var file:prepared) {
                String name=storage.save(file); written.add(name);
                var image=new ProductImages(); image.setProduct(product);
                image.setImageUrl("/api/product-images/"+name); image.setAltText(product.getName());
                image.setSortOrder(order++); image.setPrimary(primary); primary=false;
                saved.add(images.save(image));
            }
            images.flush();
            return saved.stream().map(ProductService::image).toList();
        } catch(IOException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to store product images",e); }
    }
}
