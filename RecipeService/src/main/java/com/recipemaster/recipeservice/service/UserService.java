package com.recipemaster.recipeservice.service;

import com.recipemaster.dto.UserDetailsDto;
import com.recipemaster.dto.UserDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.recipemaster.recipeservice.mapper.UserMapper.UserDTOToUserEntity;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<UserEntity> findUserEntityByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserDetailsDto findById(Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(id)));
        return new UserDetailsDto(user.getId(), user.getEmail());

    }

    public UserDetailsDto findByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow(
                () -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL.getMessage()));
        return new UserDetailsDto(user.getId(), user.getEmail());
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return new User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>()
        );
    }

    public void createNewUser(UserDto userDTO) {
        UserDto userWithPasswordDTO = new UserDto(userDTO.email(),
                passwordEncoder.encode(userDTO.password()),
                userDTO.fullName());
        UserEntity userEntity = UserDTOToUserEntity(userWithPasswordDTO);
        userRepository.save(userEntity);
    }

}