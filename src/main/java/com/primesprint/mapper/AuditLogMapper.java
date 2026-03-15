package com.primesprint.mapper;

import com.primesprint.dto.AuditLogdto;
import com.primesprint.model.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogdto toResponse(AuditLog log);

}