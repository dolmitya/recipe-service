package com.recipemaster.recipeservice.service;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.recipemaster.recipeservice.mapper.UsersProductMapper.toUsersProductEntity;

@Service
@RequiredArgsConstructor
public class UsersProductService {
    private final UsersProductRepository usersProductRepository;
    private final UserRepository userRepository;
    private final ProductElasticService productElasticService;

    public List<UserProductInfoDto> getUserProductsByUserId(Long userId) {
        List<UsersProductEntity> usersProducts = usersProductRepository.findAllByUserId(userId);

        return usersProducts.stream()
                .map(this::toUserProductInfoDto)
                .toList();
    }

    public UserProductInfoDto addProduct(Long userId, UserProductInfoDto productInputDto) {
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        ProductEntity product = productElasticService.findOrCreate(
                productInputDto.getName(),
                productInputDto.getUnit(),
                productInputDto.getCaloriesPerUnit(),
                productInputDto.getProteinsPerUnit(),
                productInputDto.getFatsPerUnit(),
                productInputDto.getCarbsPerUnit()
        );

        if (product.getUnit() != null && !product.getUnit().equals(productInputDto.getUnit())) {
            throw new IllegalArgumentException(ErrorMessage.INCORRECT_PRODUCT_UNIT.getMessage() + product.getUnit());
        }

        UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, product.getId())
                .map(existingProduct -> {
                    existingProduct.setQuantity(existingProduct.getQuantity().add(productInputDto.getQuantity()));
                    return existingProduct;
                })
                .orElseGet(() -> toUsersProductEntity(user, product, productInputDto));

        UsersProductEntity savedProduct = usersProductRepository.save(usersProduct);
        return toUserProductInfoDto(savedProduct);
    }

    public UserProductInfoDto updateProduct(Long userId, Long productId, UserProductInfoDto productInputDto) {
        UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, productId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USERS_PRODUCT_NOT_FOUND_BY_ID.getMessage()));

        if (productInputDto.getQuantity() != null) {
            usersProduct.setQuantity(productInputDto.getQuantity());
        }

        usersProductRepository.save(usersProduct);
        return toUserProductInfoDto(usersProduct);
    }

    public void deleteProduct(Long userId, Long productId) {
        usersProductRepository.deleteByUserAndProductId(userId, productId);
    }

    private UserProductInfoDto toUserProductInfoDto(UsersProductEntity usersProduct) {
        ProductEntity product = usersProduct.getProduct();
        BigDecimal quantity = usersProduct.getQuantity();
        BigDecimal caloriesPerUnit = normalizedValue(product.getCaloriesPerUnit());
        BigDecimal proteinsPerUnit = normalizedValue(product.getProteinsPerUnit());
        BigDecimal fatsPerUnit = normalizedValue(product.getFatsPerUnit());
        BigDecimal carbsPerUnit = normalizedValue(product.getCarbsPerUnit());

        return new UserProductInfoDto(
                product.getId(),
                product.getName(),
                quantity,
                product.getUnit(),
                caloriesPerUnit,
                proteinsPerUnit,
                fatsPerUnit,
                carbsPerUnit,
                caloriesPerUnit.multiply(quantity),
                proteinsPerUnit.multiply(quantity),
                fatsPerUnit.multiply(quantity),
                carbsPerUnit.multiply(quantity)
        );
    }

    private BigDecimal normalizedValue(BigDecimal value) {
        return Optional.ofNullable(value).orElse(BigDecimal.ZERO);
    }
}
