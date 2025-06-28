package com.recipemaster.recipeservice.integration;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.recipeservice.repository.ProductRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import com.recipemaster.recipeservice.service.UsersProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Transactional
class UsersProductServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UsersProductService usersProductService;

    @Autowired
    private UsersProductRepository usersProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;
    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        usersProductRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new UserEntity();
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFullName("Test User");
        testUser = userRepository.save(testUser);

        testProduct = new ProductEntity();
        testProduct.setName("Test Product");
        testProduct.setUnit("kg");
        testProduct = productRepository.save(testProduct);

        UsersProductEntity testUsersProduct = new UsersProductEntity();
        testUsersProduct.setUser(testUser);
        testUsersProduct.setProduct(testProduct);
        testUsersProduct.setQuantity(new BigDecimal("1.5"));
        usersProductRepository.save(testUsersProduct);
    }

    @Test
    void testReturnOfUserProductsByUserIdWhenUserExists() {
        List<UserProductInfoDto> result = usersProductService.getUserProductsByUserId(testUser.getId());

        assertEquals(1, result.size());
        assertEquals("Test Product", result.getFirst().getName());
        assertEquals(new BigDecimal("1.5"), result.getFirst().getQuantity());
    }

    @Test
    void testReturnOfUserProductsByUserIdWhenUserHasNoProducts() {
        usersProductRepository.deleteAll();

        List<UserProductInfoDto> result = usersProductService.getUserProductsByUserId(testUser.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void testAdditionWhenProductIsNew() {
        UserProductInfoDto newProduct = new UserProductInfoDto(
                1L,
                "New Item",
                new BigDecimal("2.0"),
                "kg");

        UserProductInfoDto result = usersProductService.addProduct(testUser.getId(), newProduct);

        assertEquals("New Item", result.getName());
        assertEquals(new BigDecimal("2.0"), result.getQuantity());

        List<UsersProductEntity> userProducts = usersProductRepository.findAllByUserId(testUser.getId());

        assertEquals(2, userProducts.size());
    }

    @Test
    void testAdditionWhenProductExists() {
        UserProductInfoDto existingProduct = new UserProductInfoDto(
                1L,
                "Test Product",
                new BigDecimal("0.5"),
                "kg");

        UserProductInfoDto result = usersProductService.addProduct(testUser.getId(), existingProduct);

        assertEquals("Test Product", result.getName());
        assertEquals(new BigDecimal("2.0"), result.getQuantity());

        UsersProductEntity updatedProduct = usersProductRepository.findProductById(testUser.getId(), testProduct.getId())
                .orElseThrow();

        assertEquals(new BigDecimal("2.0"), updatedProduct.getQuantity());
    }

    @Test
    void testUpdateWhenProductExists() {
        UserProductInfoDto updateDto = new UserProductInfoDto(
                1L,
                "Test Product",
                new BigDecimal("3.0"),
                "kg");

        UserProductInfoDto result = usersProductService.updateProduct(
                testUser.getId(),
                testProduct.getId(),
                updateDto);

        assertEquals("Test Product", result.getName());
        assertEquals(new BigDecimal("3.0"), result.getQuantity());

        UsersProductEntity updatedProduct = usersProductRepository.findProductById(testUser.getId(), testProduct.getId())
                .orElseThrow();

        assertEquals(new BigDecimal("3.0"), updatedProduct.getQuantity());
    }

    @Test
    void testDeletionWhenProductExists() {
        usersProductService.deleteProduct(testUser.getId(), testProduct.getId());

        assertFalse(usersProductRepository.findProductById(testUser.getId(), testProduct.getId()).isPresent());
    }

    @Test
    void testDeletionWhenProductNotExists() {
        assertDoesNotThrow(() ->
                usersProductService.deleteProduct(testUser.getId(), 999L));
    }
}
