package com.example.ecommerce.product;

import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.service.ProductImageService;
import com.example.ecommerce.product.storage.ProductImageStorage;
import com.example.ecommerce.product.model.Products;
import com.example.ecommerce.product.model.ProductImages;
import com.example.ecommerce.product.repository.ProductsRepository;
import com.example.ecommerce.product.repository.ProductImagesRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProductImageServiceTests {
    final ProductsRepository products=mock(ProductsRepository.class);
    final ProductImagesRepository images=mock(ProductImagesRepository.class);
    final ProductImageStorage storage=mock(ProductImageStorage.class);
    final ProductImageService service=new ProductImageService(products,images,storage);
    final MockMultipartFile file=new MockMultipartFile("files","test.png","image/png",new byte[]{1});
    @BeforeEach void setup() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        var product=new Products(); product.setId(1L); product.setName("Test");
        when(products.findForUpdate(1L)).thenReturn(Optional.of(product));
        when(storage.validate(file)).thenReturn(new ProductImageStorage.PreparedImage(new byte[]{1},"png"));
        when(storage.save(any())).thenReturn("generated.png");
        when(images.save(any())).thenAnswer(i->{ ProductImages image=i.getArgument(0); image.setId(2L); return image; });
    }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }
    @Test void assignsPrimaryAndCleansWrittenFilesOnRollback() {
        var result=service.upload(1L,List.of(file));
        assertTrue(result.getFirst().primary()); assertEquals(0,result.getFirst().sortOrder());
        assertEquals("/api/product-images/generated.png",result.getFirst().url());
        TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).delete("generated.png");
    }
    @Test void keepsFilesOnCommit() {
        service.upload(1L,List.of(file));
        TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(storage,never()).delete(any());
    }
    @Test void cleansUpWhenMetadataSaveFails() {
        doThrow(new DataIntegrityViolationException("failure")).when(images).save(any());
        assertThrows(DataIntegrityViolationException.class,()->service.upload(1L,List.of(file)));
        TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).delete("generated.png");
    }
    @Test void invalidBatchDoesNotWriteFiles() throws Exception {
        var invalid=new MockMultipartFile("files",new byte[0]);
        when(storage.validate(invalid)).thenThrow(com.example.ecommerce.common.ApiErrors.bad("Invalid image"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.upload(1L,List.of(file,invalid)));
        verify(storage,never()).save(any()); verifyNoInteractions(images);
    }
}
