package com.podo.global.security;

import com.podo.domain.user.entity.User;

public interface PrincipalUser {
    User getUser();
    Long getUserId();
}
