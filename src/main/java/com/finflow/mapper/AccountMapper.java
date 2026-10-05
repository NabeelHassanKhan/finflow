package com.finflow.mapper;

import com.finflow.dto.response.AccountResponse;
import com.finflow.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "ownerName", source = "user.fullName")
    AccountResponse toResponse(Account account);
}