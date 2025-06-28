package com.recipemaster.recipeservice.unit;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import com.recipemaster.recipeservice.service.ProductElasticService;
import com.recipemaster.recipeservice.service.UsersProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersProductServiceUnitTest {

    @Mock
    private UsersProductRepository usersProductRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UsersProductService usersProductService;

    @Mock
    private ProductElasticService productElasticService;

    @Test
    void testReturnOfUserProductsByUserIdWhenProductsAbsent() {
        Long userId = 1L;

        when(usersProductRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<UserProductInfoDto> result = usersProductService.getUserProductsByUserId(userId);

        assertTrue(result.isEmpty());
        verify(usersProductRepository).findAllByUserId(userId);
    }

    @Test
    void testAdditionOfProductWhenProductExists() {
        Long userId = 1L;
        Long productId = 1L;
        UserProductInfoDto inputDto = new UserProductInfoDto(1L, "Milk", new BigDecimal("1"), "L");

        UserEntity user = new UserEntity();
        user.setId(userId);

        ProductEntity product = new ProductEntity();
        product.setId(productId);
        product.setName("Milk");
        product.setUnit("L");

        UsersProductEntity existingProduct = new UsersProductEntity();
        existingProduct.setUser(user);
        existingProduct.setProduct(product);
        existingProduct.setQuantity(new BigDecimal("0.5"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productElasticService.findOrCreate(any(),any())).thenReturn(product);
        when(usersProductRepository.findProductById(userId, productId)).thenReturn(Optional.of(existingProduct));
        when(usersProductRepository.save(any(UsersProductEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserProductInfoDto result = usersProductService.addProduct(userId, inputDto);

        assertEquals("Milk", result.getName());
        assertEquals(new BigDecimal("1.5"), result.getQuantity());
        assertEquals("L", result.getUnit());
        verify(usersProductRepository).save(existingProduct);
    }

    @Test
    void testAdditionOfProductWhenUnitMismatch() {
        Long userId = 1L;
        UserProductInfoDto inputDto = new UserProductInfoDto(1L, "Milk", new BigDecimal("1"), "kg");

        UserEntity user = new UserEntity();
        user.setId(userId);

        ProductEntity product = new ProductEntity();
        product.setName("Milk");
        product.setUnit("L");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productElasticService.findOrCreate(any(),any())).thenReturn(product);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> usersProductService.addProduct(userId, inputDto));
        assertEquals(ErrorMessage.INCORRECT_PRODUCT_UNIT.getMessage()+"L", exception.getMessage());
    }

    @Test
    void testUpdateOfProductWhenProductNotFound() {
        Long userId = 1L;
        Long productId = 99L;
        UserProductInfoDto inputDto = new UserProductInfoDto(1L, "Milk", new BigDecimal("2"), "L");

        when(usersProductRepository.findProductById(userId, productId)).thenReturn(Optional.empty());
        
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> usersProductService.updateProduct(userId, productId, inputDto));
        assertEquals(ErrorMessage.USERS_PRODUCT_NOT_FOUND_BY_ID.getMessage(), exception.getMessage());
    }

    @Test
    void testDeletionOfProduct() {
        Long userId = 1L;
        Long productId = 1L;

        doNothing().when(usersProductRepository).deleteByUserAndProductId(userId, productId);
        
        usersProductService.deleteProduct(userId, productId);
        
        verify(usersProductRepository).deleteByUserAndProductId(userId, productId);
    }
}
