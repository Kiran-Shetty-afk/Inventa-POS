package com.zosh.controller;

import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.dto.BranchDTO;
import com.zosh.service.BranchService;
import com.zosh.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<BranchDTO> createBranch(
            @Valid @RequestBody BranchDTO dto,
            @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.createBranch(dto, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDTO> getBranch(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<BranchDTO>> getAllBranches(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long storeId) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.getAllBranchesByStoreId(storeId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchDTO> updateBranch(
            @PathVariable Long id,
            @RequestBody BranchDTO dto,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.updateBranch(id, dto, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
