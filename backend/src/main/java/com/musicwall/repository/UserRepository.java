package com.musicwall.repository;

import com.musicwall.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("""
            select user from UserEntity user
            where lower(user.username) like lower(concat('%', :query, '%'))
              and user.username <> :currentUsername
            order by user.username
            """)
    List<UserEntity> searchByUsername(
            @Param("query") String query,
            @Param("currentUsername") String currentUsername
    );
}
