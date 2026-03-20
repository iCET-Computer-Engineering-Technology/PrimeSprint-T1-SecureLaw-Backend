package com.primesprint.mapper;

import com.primesprint.dto.AuditLogDto;
import com.primesprint.model.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogDto toDto(AuditLog log);

}