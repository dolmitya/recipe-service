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
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersProductServiceTest {

    @Mock
    private UsersProductRepository usersProductRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductElasticService productElasticService;

    @InjectMocks
    private UsersProductService usersProductService;

    private UserEntity testUser;
    private ProductEntity milk;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setEmail("demo@recipe-service.local");

        milk = new ProductEntity();
        milk.setId(10L);
        milk.setName("Milk");
        milk.setUnit("ml");
        milk.setCaloriesPerUnit(new BigDecimal("0.52"));
        milk.setProteinsPerUnit(new BigDecimal("0.03"));
        milk.setFatsPerUnit(new BigDecimal("0.02"));
        milk.setCarbsPerUnit(new BigDecimal("0.05"));
    }

    @Test
    void getUserProductsByUserId_WhenProductsExist() {
        UsersProductEntity usersProduct = usersProduct(new BigDecimal("500"));
        when(usersProductRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(usersProduct));

        List<UserProductInfoDto> result = usersProductService.getUserProductsByUserId(testUser.getId());

        assertEquals(1, result.size());
        assertEquals("Milk", result.getFirst().getName());
        assertEquals(new BigDecimal("500"), result.getFirst().getQuantity());
        assertEquals(new BigDecimal("260.00"), result.getFirst().getTotalCalories());
        assertEquals(new BigDecimal("15.00"), result.getFirst().getTotalProteins());
        verify(usersProductRepository).findAllByUserId(testUser.getId());
    }

    @Test
    void getUserProductsByUserId_WhenProductsAbsent() {
        when(usersProductRepository.findAllByUserId(testUser.getId())).thenReturn(List.of());

        List<UserProductInfoDto> result = usersProductService.getUserProductsByUserId(testUser.getId());

        assertTrue(result.isEmpty());
        verify(usersProductRepository).findAllByUserId(testUser.getId());
    }

    @Test
    void addProduct_WhenProductIsNewForUser() {
        UserProductInfoDto input = productDto("Milk", new BigDecimal("300"));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        mockProductFindOrCreate(input);
        when(usersProductRepository.findProductById(testUser.getId(), milk.getId())).thenReturn(Optional.empty());
        when(usersProductRepository.save(org.mockito.ArgumentMatchers.any(UsersProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProductInfoDto result = usersProductService.addProduct(testUser.getId(), input);

        assertEquals("Milk", result.getName());
        assertEquals(new BigDecimal("300"), result.getQuantity());
        assertEquals(new BigDecimal("156.00"), result.getTotalCalories());
        verify(usersProductRepository).save(org.mockito.ArgumentMatchers.any(UsersProductEntity.class));
    }

    @Test
    void addProduct_WhenProductAlreadyExistsForUser() {
        UserProductInfoDto input = productDto("Milk", new BigDecimal("300"));
        UsersProductEntity existingProduct = usersProduct(new BigDecimal("200"));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        mockProductFindOrCreate(input);
        when(usersProductRepository.findProductById(testUser.getId(), milk.getId())).thenReturn(Optional.of(existingProduct));
        when(usersProductRepository.save(existingProduct)).thenReturn(existingProduct);

        UserProductInfoDto result = usersProductService.addProduct(testUser.getId(), input);

        assertEquals(new BigDecimal("500"), result.getQuantity());
        assertEquals(new BigDecimal("260.00"), result.getTotalCalories());
        verify(usersProductRepository).save(existingProduct);
    }

    @Test
    void addProduct_WhenUserNotFound() {
        UserProductInfoDto input = productDto("Milk", new BigDecimal("300"));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> usersProductService.addProduct(testUser.getId(), input));
        verify(productElasticService, never()).findOrCreate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void addProduct_WhenUnitDoesNotMatchExistingProduct() {
        UserProductInfoDto input = productDto("Milk", new BigDecimal("300"));
        input.setUnit("g");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        mockProductFindOrCreate(input);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usersProductService.addProduct(testUser.getId(), input)
        );

        assertEquals(ErrorMessage.INCORRECT_PRODUCT_UNIT.getMessage() + "ml", exception.getMessage());
    }

    @Test
    void updateProduct_WhenProductExists() {
        UsersProductEntity existingProduct = usersProduct(new BigDecimal("200"));
        UserProductInfoDto input = productDto("Milk", new BigDecimal("750"));
        when(usersProductRepository.findProductById(testUser.getId(), milk.getId())).thenReturn(Optional.of(existingProduct));
        when(usersProductRepository.save(existingProduct)).thenReturn(existingProduct);

        UserProductInfoDto result = usersProductService.updateProduct(testUser.getId(), milk.getId(), input);

        assertEquals(new BigDecimal("750"), result.getQuantity());
        assertEquals(new BigDecimal("390.00"), result.getTotalCalories());
        verify(usersProductRepository).save(existingProduct);
    }

    @Test
    void updateProduct_WhenProductNotFound() {
        UserProductInfoDto input = productDto("Milk", new BigDecimal("750"));
        when(usersProductRepository.findProductById(testUser.getId(), milk.getId())).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> usersProductService.updateProduct(testUser.getId(), milk.getId(), input)
        );

        assertEquals(ErrorMessage.USERS_PRODUCT_NOT_FOUND_BY_ID.getMessage(), exception.getMessage());
    }

    @Test
    void deleteProduct_WhenCalled() {
        usersProductService.deleteProduct(testUser.getId(), milk.getId());

        verify(usersProductRepository).deleteByUserAndProductId(testUser.getId(), milk.getId());
    }

    private UserProductInfoDto productDto(String name, BigDecimal quantity) {
        UserProductInfoDto dto = new UserProductInfoDto();
        dto.setName(name);
        dto.setQuantity(quantity);
        dto.setUnit("ml");
        dto.setCaloriesPerUnit(new BigDecimal("0.52"));
        dto.setProteinsPerUnit(new BigDecimal("0.03"));
        dto.setFatsPerUnit(new BigDecimal("0.02"));
        dto.setCarbsPerUnit(new BigDecimal("0.05"));
        return dto;
    }

    private UsersProductEntity usersProduct(BigDecimal quantity) {
        UsersProductEntity usersProduct = new UsersProductEntity();
        usersProduct.setUser(testUser);
        usersProduct.setProduct(milk);
        usersProduct.setQuantity(quantity);
        return usersProduct;
    }

    private void mockProductFindOrCreate(UserProductInfoDto input) {
        when(productElasticService.findOrCreate(
                eq(input.getName()),
                eq(input.getUnit()),
                eq(input.getCaloriesPerUnit()),
                eq(input.getProteinsPerUnit()),
                eq(input.getFatsPerUnit()),
                eq(input.getCarbsPerUnit())
        )).thenReturn(milk);
    }
}
