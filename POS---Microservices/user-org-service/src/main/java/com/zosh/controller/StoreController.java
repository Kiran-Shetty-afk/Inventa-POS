package com.zosh.controller;

import com.zosh.domain.StoreStatus;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.mapper.StoreMapper;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.StoreDTO;
import com.zosh.payload.dto.UserDTO;
import com.zosh.payload.response.ApiResponse;
import com.zosh.service.StoreService;
import com.zosh.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<StoreDTO> createStore(@Valid @RequestBody StoreDTO storeDto,
                                                @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(storeService.createStore(storeDto, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDTO> getStoreById(@PathVariable Long id) throws ResourceNotFoundException {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreDTO> updateStore(
            @PathVariable Long id,
            @RequestBody StoreDTO storeDto) throws ResourceNotFoundException, UserException {
        return ResponseEntity.ok(storeService.updateStore(id, storeDto));
    }

    @DeleteMapping()
    public ResponseEntity<ApiResponse> deleteStore() throws ResourceNotFoundException, UserException {
        storeService.deleteStore();
        return ResponseEntity.ok(new ApiResponse("store deleted successfully"));
    }

    @GetMapping("/admin")
    public ResponseEntity<StoreDTO> getStoresByAdminId() throws UserException {
        Store store = storeService.getStoreByAdminId();
        return ResponseEntity.ok(StoreMapper.toDto(store));
    }

    @GetMapping("/employee")
    public ResponseEntity<StoreDTO> getStoresByEmployee() throws UserException {
        StoreDTO store = storeService.getStoreByEmployee();
        return ResponseEntity.ok(store);
    }

    @GetMapping("/{storeId}/employee/list")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_MANAGER', 'ROLE_STORE_ADMIN')")
    public ResponseEntity<List<UserDTO>> getStoreEmployeeList(
            @PathVariable Long storeId) throws UserException {
        List<UserDTO> users = storeService.getEmployeesByStore(storeId);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/add/employee")
    @PreAuthorize("hasAnyAuthority('STORE_MANAGER','STORE_ADMIN')")
    public ResponseEntity<UserDTO> addEmployee(
            @RequestBody UserDTO userDTO) throws UserException {
        UserDTO user = storeService.addEmployee(null, userDTO);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<StoreDTO>> getAllStores(
            @RequestParam(required = false) StoreStatus status) {
        return ResponseEntity.ok(storeService.getAllStores(status));
    }

    @PutMapping("/{storeId}/moderate")
    public StoreDTO moderateStore(
            @PathVariable Long storeId,
            @RequestParam StoreStatus action) throws ResourceNotFoundException {
        return storeService.moderateStore(storeId, action);
    }
}
