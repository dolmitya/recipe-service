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

import java.util.List;
import java.util.NoSuchElementException;

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
                .map(up -> new UserProductInfoDto(
                        up.getProduct().getId(),
                        up.getProduct().getName(),
                        up.getQuantity(),
                        up.getProduct().getUnit()
                ))
                .toList();
    }

    public UserProductInfoDto addProduct(Long userId, UserProductInfoDto productInputDto) {
        productInputDto.setName(productInputDto.getName().toLowerCase());
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage()));

        ProductEntity product = productElasticService.findOrCreate(
                productInputDto.getName(),
                productInputDto.getUnit()
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

        return new UserProductInfoDto(
                savedProduct.getProduct().getId(),
                savedProduct.getProduct().getName(),
                savedProduct.getQuantity(),
                savedProduct.getProduct().getUnit()
        );
    }

    public UserProductInfoDto updateProduct(Long userId, Long productId, UserProductInfoDto productInputDto) {
        UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, productId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USERS_PRODUCT_NOT_FOUND_BY_ID.getMessage()));
        usersProduct.setQuantity(productInputDto.getQuantity());

        usersProductRepository.save(usersProduct);

        return new UserProductInfoDto(
                usersProduct.getProduct().getId(),
                usersProduct.getProduct().getName(),
                usersProduct.getQuantity(),
                usersProduct.getProduct().getUnit()
        );
    }

    public void deleteProduct(Long userId, Long productId) {
        usersProductRepository.deleteByUserAndProductId(userId, productId);
    }
}
