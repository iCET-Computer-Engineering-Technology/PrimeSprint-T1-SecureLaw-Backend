package com.primesprint.controller;

import com.primesprint.model.dto.Page;
import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.PageRequest;
import com.primesprint.model.dto.request.UserCreateRequest;
import com.primesprint.model.dto.request.UserUpdateRequest;
import com.primesprint.model.dto.response.ApiResponse;
import com.primesprint.model.dto.response.PageResponse;
import com.primesprint.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search
    ) {
        PageRequest pageRequest = new PageRequest(page, size, sort, direction);
        Page<UserDto> userPage = adminUserService.getUsers(pageRequest, search);
        PageResponse<UserDto> pageResponse = PageResponse.of(userPage);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Users retrieved successfully", pageResponse)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody UserCreateRequest request) {
        UserDto createdUser = adminUserService.createUser(request);
        URI location = URI.create("/api/admin/users/" + createdUser.getId());
        return ResponseEntity.created(location).body(
                ApiResponse.success(201, "User created successfully", createdUser)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@RequestBody UserUpdateRequest request, @PathVariable UUID id) {
        UserDto updatedUser = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(200, "User updated successfully", updatedUser)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
