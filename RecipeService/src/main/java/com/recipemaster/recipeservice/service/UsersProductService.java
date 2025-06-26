package com.recipemaster.recipeservice.service;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.ProductRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

import static com.recipemaster.recipeservice.mapper.ProductMapper.productDTOToProductEntity;
import static com.recipemaster.recipeservice.mapper.UsersProductMapper.toUsersProductEntity;

@Service
@RequiredArgsConstructor
public class UsersProductService {
    private final UsersProductRepository usersProductRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<UserProductInfoDto> getUserProductsByUserId(Long userId) {
        List<UsersProductEntity> usersProducts = usersProductRepository.findAllByUserId(userId);

        return usersProducts.stream()
                .map(up -> new UserProductInfoDto(
                        up.getProduct().getName(),
                        up.getQuantity(),
                        up.getProduct().getUnit()
                ))
                .toList();
    }

    public UserProductInfoDto addProduct(Long userId, UserProductInfoDto productInputDto) {
        // TODO: Добавить продукт в холодильник (связать с пользователем) здесь везде нужен elastic его пока нет)
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage()));

        ProductEntity product = productRepository.findByName(productInputDto.getProductName())
                .orElseGet(() -> productRepository.save(productDTOToProductEntity(productInputDto)));

        if (product.getUnit() != null && !product.getUnit().equals(productInputDto.getUnit())) {
            throw new IllegalArgumentException(ErrorMessage.INCORRECT_PRODUCT_UNIT.getMessage());
        }

        UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, product.getId())
                .map(existingProduct -> {
                    existingProduct.setQuantity(existingProduct.getQuantity().add(productInputDto.getQuantity()));
                    return existingProduct;
                })
                .orElseGet(() -> toUsersProductEntity(user, product, productInputDto));

        UsersProductEntity savedProduct = usersProductRepository.save(usersProduct);

        return new UserProductInfoDto(
                savedProduct.getProduct().getName(),
                savedProduct.getQuantity(),
                savedProduct.getProduct().getUnit()
        );
    }

    public UserProductInfoDto updateProduct(Long userId, Long productId, UserProductInfoDto productInputDto) {
        // TODO: Обновить продукт (проверь, принадлежит ли продукт пользователю)
        UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, productId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.USERS_PRODUCT_NOT_FOUND_BY_ID.getMessage()));
        usersProduct.setQuantity(productInputDto.getQuantity());

        usersProductRepository.save(usersProduct);

        return new UserProductInfoDto(
                usersProduct.getProduct().getName(),
                usersProduct.getQuantity(),
                usersProduct.getProduct().getUnit()
        );
    }

    public void deleteProduct(Long userId, Long productId) {
        // TODO: Удалить продукт (проверь, принадлежит ли продукт пользователю)
        usersProductRepository.deleteByUserAndProductId(userId, productId);
    }
}
