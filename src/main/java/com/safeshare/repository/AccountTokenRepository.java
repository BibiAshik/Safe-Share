package com.safeshare.repository;

import com.safeshare.entity.AccountToken;
import com.safeshare.entity.AccountTokenType;
import com.safeshare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {
    Optional<AccountToken> findByTokenAndTokenType(String token, AccountTokenType tokenType);
    void deleteByUserAndTokenType(User user, AccountTokenType tokenType);
}
