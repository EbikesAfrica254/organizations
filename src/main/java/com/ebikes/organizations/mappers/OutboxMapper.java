package com.ebikes.organizations.mappers;

import org.mapstruct.Mapper;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.dtos.responses.outbox.OutboxResponse;

@Mapper(componentModel = "spring")
public interface OutboxMapper {

  OutboxResponse toResponse(Outbox outbox);
}
