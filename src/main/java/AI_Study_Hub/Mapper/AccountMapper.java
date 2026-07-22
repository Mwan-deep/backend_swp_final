package AI_Study_Hub.Mapper;

import AI_Study_Hub.dto.request.AccountCreateRequest;
import AI_Study_Hub.dto.request.AccountUpdateRequest;
import AI_Study_Hub.dto.response.AccountResponse;
import AI_Study_Hub.entity.Account;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(componentModel = "spring")
public interface AccountMapper {
    Account toAccount(AccountCreateRequest request);
    AccountResponse toAccountResponse(Account account);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toUpdateAccount(AccountUpdateRequest request, @MappingTarget Account account);
}
