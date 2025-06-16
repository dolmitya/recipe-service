package com.recipemaster.recipeservice.services;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.recipeservice.repositories.UsersProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsersProductService {
    private final UsersProductRepository usersProductRepository;

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
        return null;
    }

    public UserProductInfoDto updateProduct(Long userId, Long productId, UserProductInfoDto productInputDto) {
        // TODO: Обновить продукт (проверь, принадлежит ли продукт пользователю)
        return null;
    }

    public void deleteProduct(Long userId, Long productId) {
        // TODO: Удалить продукт (проверь, принадлежит ли продукт пользователю)
    }
}
