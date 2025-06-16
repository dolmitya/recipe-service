package com.recipemaster.recipeservice.repositories;

import com.recipemaster.entities.UsersProductEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsersProductRepository  extends CrudRepository<UsersProductEntity, Long> {
    @Query("SELECT up FROM UsersProductEntity up WHERE up.user.id = :userId")
    List<UsersProductEntity> findAllByUserId(Long userId);
}
